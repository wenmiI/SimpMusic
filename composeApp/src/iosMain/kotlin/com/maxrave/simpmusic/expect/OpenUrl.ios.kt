package com.maxrave.simpmusic.expect

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}

actual fun shareUrl(
    title: String,
    url: String,
) {
    val item: Any = NSURL.URLWithString(url) ?: url
    presentFromTop(UIActivityViewController(activityItems = listOf(item), applicationActivities = null))
}
