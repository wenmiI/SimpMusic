package com.maxrave.simpmusic.expect

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

/**
 * The view controller everything modal is presented from: the top of the key window's stack, so a
 * share sheet opened while another sheet is up lands on top of it instead of failing silently.
 */
internal fun topViewController(): UIViewController? {
    val window =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow
    var top = window?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}

/** Presents [controller] from [topViewController], anchored to the window's centre on iPad. */
@OptIn(ExperimentalForeignApi::class)
internal fun presentFromTop(controller: UIViewController) {
    val top = topViewController() ?: return
    controller.popoverPresentationController?.let { popover ->
        popover.sourceView = top.view
        popover.sourceRect = top.view.bounds
        popover.permittedArrowDirections = 0u
    }
    top.presentViewController(controller, animated = true, completion = null)
}
