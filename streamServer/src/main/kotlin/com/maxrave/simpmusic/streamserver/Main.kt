package com.maxrave.simpmusic.streamserver

import com.maxrave.kotlinytmusicscraper.extractor.Extractor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.call
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * SimpMusic stream host — resolves a YouTube audio stream with the full tiered extractor (the same
 * one the Desktop/Android app uses, which gets uncapped URLs) and proxies the bytes to the caller.
 *
 * Why a proxy and not just the URL: googlevideo signs the requester's IP into the URL (`ip` is in
 * `sparams`), so a URL resolved here would 403 from any other device. The caller (the iOS app)
 * therefore streams the audio FROM this host, which fetches it from googlevideo itself.
 *
 *   GET /health            -> "ok"
 *   GET /audio/<videoId>   -> audio/mp4 (itag 140), with Range support
 */

private const val CHUNK = 1L * 1024 * 1024 // googlevideo serves a limited span per request; 1 MB is safe.

private val extractor = Extractor().apply { init() }
private val client =
    HttpClient(ClientCIO) {
        install(HttpTimeout) { requestTimeoutMillis = 120_000; socketTimeoutMillis = 60_000 }
    }

/** A resolved audio URL and its content length, cached until the URL is near expiry. */
private class Resolved(val url: String, val length: Long, val expiresAtSec: Long)

private val cache = HashMap<String, Resolved>()
private val cacheLock = Mutex()

private suspend fun resolve(videoId: String): Resolved? {
    val now = System.currentTimeMillis() / 1000
    cacheLock.withLock {
        cache[videoId]?.let { if (it.expiresAtSec - 300 > now) return it else cache.remove(videoId) }
    }
    val url =
        withContext(Dispatchers.IO) { runCatching { extractor.newPipePlayer(videoId) }.getOrNull() }
            ?.firstOrNull { it.first == 140 }
            ?.second ?: return null
    val params = Url(url).parameters
    val resolved =
        Resolved(
            url = url,
            length = params["clen"]?.toLongOrNull() ?: -1L,
            expiresAtSec = params["expire"]?.toLongOrNull() ?: (now + 5 * 3600),
        )
    cacheLock.withLock { cache[videoId] = resolved }
    return resolved
}

/** Parses a `bytes=start-end` request header into an absolute [start, endInclusive] within [total]. */
private fun parseRange(
    header: String?,
    total: Long,
): Pair<Long, Long> {
    if (header == null || !header.startsWith("bytes=") || total <= 0) return 0L to (total - 1).coerceAtLeast(0)
    val spec = header.removePrefix("bytes=").substringBefore(',')
    val start = spec.substringBefore('-').toLongOrNull() ?: 0L
    val end = spec.substringAfter('-').toLongOrNull() ?: (total - 1)
    return start.coerceIn(0, total - 1) to end.coerceIn(start, total - 1)
}

fun main() {
    val port = System.getenv("STREAM_PORT")?.toIntOrNull() ?: 8120
    val host = System.getenv("STREAM_HOST") ?: "0.0.0.0"
    println("simpmusic-stream-host on $host:$port")
    embeddedServer(ServerCIO, host = host, port = port) {
        routing {
            get("/health") { call.respondText("ok") }
            get("/audio/{videoId}") {
                val videoId = call.parameters["videoId"].orEmpty()
                val resolved = resolve(videoId)
                if (resolved == null) {
                    call.respond(HttpStatusCode.BadGateway, "could not resolve $videoId")
                    return@get
                }
                val total = resolved.length
                val rangeHeader = call.request.headers[HttpHeaders.Range]
                val (start, end) = parseRange(rangeHeader, total)
                val ranged = rangeHeader != null && total > 0

                call.response.header(HttpHeaders.AcceptRanges, "bytes")
                if (ranged) call.response.header(HttpHeaders.ContentRange, "bytes $start-$end/$total")
                val length = if (total > 0) end - start + 1 else null

                call.respondBytesWriter(
                    contentType = ContentType.parse("audio/mp4"),
                    status = if (ranged) HttpStatusCode.PartialContent else HttpStatusCode.OK,
                    contentLength = length,
                ) {
                    var offset = if (total > 0) start else 0L
                    val limit = if (total > 0) end else Long.MAX_VALUE
                    while (offset <= limit) {
                        val to = if (total > 0) minOf(offset + CHUNK - 1, limit) else offset + CHUNK - 1
                        val resp = client.get(resolved.url) { header(HttpHeaders.Range, "bytes=$offset-$to") }
                        if (resp.status != HttpStatusCode.PartialContent && resp.status != HttpStatusCode.OK) break
                        val copied = resp.bodyAsChannel().copyTo(this)
                        if (copied == 0L) break
                        offset += copied
                    }
                }
            }
        }
    }.start(wait = true)
}
