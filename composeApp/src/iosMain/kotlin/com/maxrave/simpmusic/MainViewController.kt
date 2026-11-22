package com.maxrave.simpmusic

import androidx.compose.ui.window.ComposeUIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import kotlin.native.ref.WeakReference

/** What iosApp's SwiftUI scene shows: the whole app, in [AppViewController]. */
fun MainViewController(): UIViewController {
    startSimpMusic()
    return AppViewController(ComposeUIViewController { App() })
}

/**
 * Hosts the Compose view controller and answers for the status bar, which on iOS belongs to the
 * root view controller: Compose itself has no say in it, and the app theme can differ from the
 * system's (see SystemBarAppearanceEffect).
 */
@OptIn(ExperimentalForeignApi::class)
class AppViewController internal constructor(
    private val content: UIViewController,
) : UIViewController(nibName = null, bundle = null) {
    private var darkTheme = true

    override fun viewDidLoad() {
        super.viewDidLoad()
        setCurrentAppViewController(this)
        addChildViewController(content)
        content.view.setFrame(view.bounds)
        content.view.autoresizingMask = platform.UIKit.UIViewAutoresizingFlexibleWidth or platform.UIKit.UIViewAutoresizingFlexibleHeight
        view.addSubview(content.view)
        content.didMoveToParentViewController(this)
    }

    override fun preferredStatusBarStyle(): UIStatusBarStyle = if (darkTheme) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent

    internal fun setDarkTheme(isDark: Boolean) {
        if (darkTheme == isDark) return
        darkTheme = isDark
        setNeedsStatusBarAppearanceUpdate()
    }

    companion object {
        /** The controller on screen, for SystemBarAppearanceEffect; null before the first viewDidLoad. */
        internal val current: AppViewController?
            get() = currentAppViewController()
    }
}

// Kept out of the companion: Kotlin/Native allows no fields in the companion of an ObjC subclass.
@OptIn(ExperimentalNativeApi::class)
private var currentRef: WeakReference<AppViewController>? = null

@OptIn(ExperimentalNativeApi::class)
private fun currentAppViewController(): AppViewController? = currentRef?.get()

@OptIn(ExperimentalNativeApi::class)
private fun setCurrentAppViewController(controller: AppViewController) {
    currentRef = WeakReference(controller)
}
