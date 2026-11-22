package com.maxrave.simpmusic

import com.eygraber.uri.Uri
import com.maxrave.common.AppIdentity
import com.maxrave.data.db.documentDirectory
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.domain.repository.AccountSyncRepository
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.utils.ComposeResUtils
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin
import org.simpmusic.discord.configDiscord
import org.simpmusic.lastfm.configLastfm
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.explicit_content_blocked
import simpmusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings

private var started = false

/**
 * What runDesktopApp does before its first window, for iOS: logs, Koin, the services that need their
 * keys before anything asks for them, and the player's hooks into the UI. Called once, from
 * [MainViewController], before the first composition.
 *
 * Left out on purpose: the update check (it offers Desktop and Android builds), Sentry (no iOS SDK
 * here) and the single-instance guard (iOS runs one instance by itself).
 */
internal fun startSimpMusic() {
    if (started) return
    started = true

    Logger.enableFileLogging(documentDirectory() + "/logs")

    // Before startKoin: DiscordRepository reads it once, when it is created.
    configDiscord(BuildKonfig.discordClientId)

    startKoin {
        loadAllModules(
            AppIdentity(
                applicationId = NSBundle.mainBundle.bundleIdentifier ?: "com.maxrave.simpmusic",
                versionName = BuildKonfig.versionName,
                platform = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
                isDevBuild = false,
            ),
        )
        loadKoinModules(viewModelModule)
    }

    VersionManager.initialize()
    configLastfm(BuildKonfig.lastfmApiKey, BuildKonfig.lastfmSecret)

    val mediaPlayerHandler = getKoin().get<MediaPlayerHandler>()
    mediaPlayerHandler.showToast = { type ->
        showToast(
            when (type) {
                ToastType.ExplicitContent -> {
                    runBlocking { getString(Res.string.explicit_content_blocked) }
                }

                is ToastType.PlayerError -> {
                    runBlocking { getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, type.error) }
                }

                is ToastType.SponsorBlockSkip -> {
                    runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.SPONSOR_BLOCK_SKIP, type.category) }
                }
            },
        )
    }
    mediaPlayerHandler.pushPlayerError = { error ->
        Logger.e("Player", "Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
    }

    getKoin().get<AccountSyncRepository>().start()
}

/**
 * A URL the system opened the app with (SwiftUI's onOpenURL): simpmusic://, the Last.fm callback
 * wordbyword://, or a shared music.youtube.com link. Handed over raw, the way MainActivity hands
 * Android's intent data, so App.kt routes it the same way on both.
 */
fun handleOpenUrl(url: String) {
    getKoin().get<SharedViewModel>().setIntent(
        GenericIntent(
            action = "android.intent.action.VIEW",
            data = runCatching { Uri.parse(url) }.getOrNull(),
        ),
    )
}
