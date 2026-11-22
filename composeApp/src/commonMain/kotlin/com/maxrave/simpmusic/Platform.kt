package com.maxrave.simpmusic

sealed class Platform {
    object Android : Platform()
    object iOS : Platform()
    object Desktop : Platform()

    fun osName(): String = when (this) {
        Android -> "android"
        iOS -> "iOS"
        Desktop -> desktopOsName()
    }
}

expect fun getPlatform(): Platform

/** The desktop OS by name; only the JVM ever answers, the other platforms are never Desktop. */
internal expect fun desktopOsName(): String