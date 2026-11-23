package com.maxrave.simpmusic

import androidx.compose.runtime.Composable
import coil3.EventListener
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.compose.preferEndFirstIntrinsicSize
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.utils.NetworkFirstInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import okio.FileSystem

/**
 * The image loader DesktopApp sets up, for iOS, on the Darwin engine. Left to itself Coil builds its
 * own `HttpClient()`, which takes whichever Ktor engine registered first; this app links CIO as well
 * as Darwin, and on the first device run (2026-11-23) no artwork loaded at all. Darwin goes through
 * NSURLSession, as every other request on iOS does. A failed image is logged with its host, so the
 * app log answers why the next time one does not show.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun IosImageLoader() {
    val context = LocalPlatformContext.current
    setSingletonImageLoaderFactory {
        lateinit var imageLoader: ImageLoader
        imageLoader =
            ImageLoader
                .Builder(context)
                .components {
                    add(NetworkFirstInterceptor { imageLoader })
                    add(KtorNetworkFetcherFactory(httpClient = { HttpClient(Darwin) }))
                }.eventListener(ImageFailureLogger)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .diskCache(
                    DiskCache
                        .Builder()
                        .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                        .maxSizeBytes(512L * 1024 * 1024)
                        .build(),
                ).crossfade(true)
                .preferEndFirstIntrinsicSize(true)
                .build()
        imageLoader
    }
}

private object ImageFailureLogger : EventListener() {
    override fun onError(
        request: ImageRequest,
        result: ErrorResult,
    ) {
        val host = request.data.toString().substringAfter("://", "").substringBefore('/')
        Logger.w("Coil", "image from ${host.ifEmpty { "a local source" }} failed: ${result.throwable::class.simpleName}: ${result.throwable.message}")
    }
}
