package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.WebKit.WKWebsiteDataTypeCookies
import platform.darwin.NSObject
import kotlin.concurrent.AtomicReference

/**
 * The cookies of the default WKWebsiteDataStore, as last read.
 *
 * WKHTTPCookieStore only answers asynchronously, while the login screens ask synchronously right
 * after a page finishes. So every finished navigation reads the store into this snapshot BEFORE the
 * screen hears about it (see [NavigationDelegate]), and [getCookie] answers from the snapshot.
 */
private val cookieSnapshot = AtomicReference<List<NSHTTPCookie>>(emptyList())

actual fun createWebViewCookieManager(): WebViewCookieManager =
    object : WebViewCookieManager {
        // The same "a=1; b=2" Android's CookieManager.getCookie returns: every unexpired cookie
        // whose domain and path cover this url.
        override fun getCookie(url: String): String {
            val target = NSURL.URLWithString(url) ?: return ""
            val host = target.host?.lowercase() ?: return ""
            val path = target.path?.ifEmpty { "/" } ?: "/"
            val now = NSDate().timeIntervalSince1970
            return cookieSnapshot.value
                .filter { cookie ->
                    val domain = cookie.domain.lowercase().removePrefix(".")
                    (host == domain || host.endsWith(".$domain")) &&
                        path.startsWith(cookie.path) &&
                        (cookie.expiresDate?.let { it.timeIntervalSince1970 > now } ?: true)
                }.joinToString("; ") { "${it.name}=${it.value}" }
        }

        override fun removeAllCookies() {
            cookieSnapshot.value = emptyList()
            WKWebsiteDataStore.defaultDataStore().removeDataOfTypes(
                setOf(WKWebsiteDataTypeCookies),
                modifiedSince = NSDate.dateWithTimeIntervalSince1970(0.0),
                completionHandler = {},
            )
        }
    }

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    clearCookies: Boolean,
    onPageFinished: (String) -> Unit,
) {
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    // UIKit holds a web view's delegates weakly.
    val delegate = remember { NavigationDelegate(state) { currentOnPageFinished(it) } }
    Box {
        UIKitView(
            factory = {
                val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = WKWebViewConfiguration())
                webView.navigationDelegate = delegate
                val load = { webView.loadRequest(NSURLRequest.requestWithURL(NSURL.URLWithString(initUrl)!!)) }
                if (clearCookies) {
                    cookieSnapshot.value = emptyList()
                    WKWebsiteDataStore.defaultDataStore().removeDataOfTypes(
                        setOf(WKWebsiteDataTypeCookies),
                        modifiedSince = NSDate.dateWithTimeIntervalSince1970(0.0),
                        completionHandler = { load() },
                    )
                } else {
                    load()
                }
                webView
            },
            modifier = Modifier.fillMaxSize(),
        )
        aboveContent()
    }
}

/**
 * Discord keeps its token in localStorage; once the web app is open, a script copies it out of a
 * fresh iframe's storage (the page hides its own) and posts it back — the same trick Android plays
 * with an alert().
 */
@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun DiscordWebView(
    state: MutableState<WebViewState>,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onLoginDone: (token: String) -> Unit,
) {
    val currentOnLoginDone by rememberUpdatedState(onLoginDone)
    val handler = remember { TokenMessageHandler { currentOnLoginDone(it) } }
    val delegate =
        remember {
            NavigationDelegate(state) { url ->
                if (url.endsWith("/app")) handler.webView?.evaluateJavaScript(DISCORD_TOKEN_SCRIPT, completionHandler = null)
            }
        }
    Box {
        UIKitView(
            factory = {
                val contentController = WKUserContentController().apply { addScriptMessageHandler(handler, name = DISCORD_MESSAGE) }
                val configuration = WKWebViewConfiguration().apply { userContentController = contentController }
                WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).also { webView ->
                    handler.webView = webView
                    webView.navigationDelegate = delegate
                    webView.loadRequest(NSURLRequest.requestWithURL(NSURL.URLWithString("https://discord.com/login")!!))
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.configuration.userContentController.removeScriptMessageHandlerForName(DISCORD_MESSAGE) },
        )
        aboveContent()
    }
}

private const val DISCORD_MESSAGE = "simpmusicToken"
private const val DISCORD_TOKEN_SCRIPT =
    "(function(){var i=document.createElement('iframe');document.body.appendChild(i);" +
        "window.webkit.messageHandlers.$DISCORD_MESSAGE.postMessage(i.contentWindow.localStorage.token.slice(1,-1))})()"

@OptIn(ExperimentalForeignApi::class)
private class NavigationDelegate(
    private val state: MutableState<WebViewState>,
    private val onPageFinished: (String) -> Unit,
) : NSObject(),
    WKNavigationDelegateProtocol {
    @kotlinx.cinterop.ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didStartProvisionalNavigation: WKNavigation?,
    ) {
        state.value = WebViewState.Loading(0)
    }

    @kotlinx.cinterop.ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFinishNavigation: WKNavigation?,
    ) {
        val url = webView.URL?.absoluteString ?: return
        webView.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies ->
            cookieSnapshot.value = cookies?.filterIsInstance<NSHTTPCookie>().orEmpty()
            state.value = WebViewState.Finished
            onPageFinished(url)
        }
    }
}

private class TokenMessageHandler(
    private val onToken: (String) -> Unit,
) : NSObject(),
    WKScriptMessageHandlerProtocol {
    var webView: WKWebView? = null

    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        (didReceiveScriptMessage.body as? String)?.takeIf { it.isNotEmpty() }?.let(onToken)
    }
}
