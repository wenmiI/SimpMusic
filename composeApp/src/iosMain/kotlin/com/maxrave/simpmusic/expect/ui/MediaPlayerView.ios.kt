package com.maxrave.simpmusic.expect.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.simpmusic.ui.theme.typo
import com.simpmusic.media_ios.player.AvPlayerAdapter
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVPlayerLooper
import platform.AVFoundation.AVQueuePlayer
import platform.AVFoundation.muted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView

private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")

/**
 * One looping, muted clip with its own player: a Spotify canvas or Apple Music's animated artwork.
 * AVPlayerLooper keeps the loop seamless, which seeking back at the end does not.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MediaPlayerView(
    url: String,
    modifier: Modifier,
    cropToBounds: Boolean,
    onFirstFrame: () -> Unit,
) {
    val player = remember(url) { AVQueuePlayer().apply { muted = true } }
    val looper =
        remember(url) {
            NSURL.URLWithString(url)?.let { AVPlayerLooper.playerLooperWithPlayer(player, templateItem = AVPlayerItem.playerItemWithURL(it)) }
        }
    DisposableEffect(player) {
        player.play()
        onDispose {
            looper?.disableLooping()
            player.pause()
            player.removeAllItems()
        }
    }
    PlayerSurface(player, modifier, cropToBounds, onFirstFrame)
}

@Composable
actual fun rememberVideoAspectRatio(playerName: String): Float? {
    val adapter = koinInject<AvPlayerAdapter>()
    return adapter.videoAspectRatio.collectAsState().value
}

/** The main player's picture with the lyrics as subtitles, or the artwork while it plays audio. */
@Composable
actual fun MediaPlayerViewWithSubtitle(
    modifier: Modifier,
    playerName: String,
    shouldPip: Boolean,
    shouldShowSubtitle: Boolean,
    shouldScaleDownSubtitle: Boolean,
    isInPipMode: Boolean,
    timelineState: TimeLine,
    lyricsData: Lyrics?,
    translatedLyricsData: Lyrics?,
    mainTextStyle: TextStyle,
    translatedTextStyle: TextStyle,
) {
    val adapter = koinInject<AvPlayerAdapter>()
    val mediaPlayerHandler = koinInject<MediaPlayerHandler>()
    // Subtitles are lyrics like any other, so they take the same audio-delay correction.
    val lyricsOffsetMs by koinInject<DataStoreManager>().lyricsOffsetMs.collectAsState(0)
    val state by mediaPlayerHandler.nowPlayingState.collectAsState()
    val isVideo by adapter.isVideo.collectAsState()
    val artworkUri = state.songEntity?.thumbnails

    var currentLineIndex by rememberSaveable { mutableIntStateOf(-1) }
    var currentTranslatedLineIndex by rememberSaveable { mutableIntStateOf(-1) }

    // MediaPlayerViewWithSubtitleJvm's line tracking, unchanged.
    LaunchedEffect(key1 = timelineState, key2 = lyricsOffsetMs) {
        val lines = lyricsData?.lines ?: return@LaunchedEffect
        val translatedLines = translatedLyricsData?.lines
        val nowMs = timelineState.current - lyricsOffsetMs
        if (nowMs > 0L) {
            lines.indices.forEach { i ->
                val startTimeMs = lines[i].startTimeMs.toLong()
                val endTimeMs = if (i < lines.size - 1) lines[i + 1].startTimeMs.toLong() else startTimeMs + 60000
                if (nowMs in startTimeMs..endTimeMs) currentLineIndex = i
            }
            translatedLines?.indices?.forEach { i ->
                val startTimeMs = translatedLines[i].startTimeMs.toLong()
                val endTimeMs = if (i < translatedLines.size - 1) translatedLines[i + 1].startTimeMs.toLong() else startTimeMs + 60000
                if (nowMs in startTimeMs..endTimeMs) currentTranslatedLineIndex = i
            }
            if (lines.isNotEmpty() && (nowMs in (0..(lines.getOrNull(0)?.startTimeMs ?: "0").toLong()))) {
                currentLineIndex = -1
                currentTranslatedLineIndex = -1
            }
        } else {
            currentLineIndex = -1
            currentTranslatedLineIndex = -1
        }
    }

    Box(modifier = modifier.graphicsLayer { clip = true }, contentAlignment = Alignment.Center) {
        if (!isVideo) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(artworkUri)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(artworkUri)
                        .crossfade(550)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.fillMaxHeight().align(Alignment.Center),
            )
        } else {
            PlayerSurface(adapter.avPlayer, Modifier.fillMaxSize(), cropToBounds = false, onFirstFrame = {})
        }
        if (lyricsData != null && shouldShowSubtitle) {
            Crossfade(currentLineIndex != -1, modifier = Modifier.align(Alignment.BottomCenter).fillMaxSize()) {
                val lines = lyricsData.lines ?: return@Crossfade
                if (it) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(bottom = if (shouldScaleDownSubtitle) 10.dp else 40.dp)
                            .align(Alignment.BottomCenter),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.fillMaxWidth(0.7f)) {
                            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text =
                                        lines
                                            .getOrNull(currentLineIndex)
                                            ?.words
                                            ?.replace(RICH_SYNC_TIMESTAMP_REGEX, "")
                                            ?.trim() ?: return@Crossfade,
                                    style = typo().bodyLarge.scaledDown(shouldScaleDownSubtitle),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(4.dp).background(Color.Black.copy(alpha = 0.5f)).wrapContentWidth(),
                                )
                                Crossfade(translatedLyricsData?.lines != null, label = "") { translate ->
                                    val translateLines = translatedLyricsData?.lines ?: return@Crossfade
                                    if (translate) {
                                        Text(
                                            text = translateLines.getOrNull(currentTranslatedLineIndex)?.words ?: return@Crossfade,
                                            style = typo().bodyMedium.scaledDown(shouldScaleDownSubtitle),
                                            color = Color.Yellow,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).wrapContentWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun TextStyle.scaledDown(scaleDown: Boolean): TextStyle = if (scaleDown) copy(fontSize = fontSize * 0.8f) else this

/** [player]'s picture, through an AVPlayerLayer kept the size of its view. */
@OptIn(ExperimentalForeignApi::class)
@Composable
private fun PlayerSurface(
    player: AVPlayer,
    modifier: Modifier,
    cropToBounds: Boolean,
    onFirstFrame: () -> Unit,
) {
    val currentOnFirstFrame by rememberUpdatedState(onFirstFrame)
    var view by remember { mutableStateOf<PlayerLayerView?>(null) }
    UIKitView(
        factory = { PlayerLayerView(player).also { view = it } },
        modifier = modifier,
        update = { it.playerLayer.videoGravity = if (cropToBounds) AVLayerVideoGravityResizeAspectFill else AVLayerVideoGravityResizeAspect },
    )
    // AVPlayerLayer says when it has a frame to show only through KVO; reading it is cheap.
    LaunchedEffect(view) {
        val layer = view?.playerLayer ?: return@LaunchedEffect
        while (!layer.readyForDisplay) delay(50)
        currentOnFirstFrame()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PlayerLayerView(
    player: AVPlayer,
) : UIView(frame = CGRectZero.readValue()) {
    val playerLayer: AVPlayerLayer = AVPlayerLayer.playerLayerWithPlayer(player)

    init {
        // The black shutter MediaPlayerView promises until the first frame.
        backgroundColor = UIColor.blackColor
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // Without the transaction the layer animates to its new frame and lags the view.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}
