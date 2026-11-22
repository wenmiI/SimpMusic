package com.maxrave.simpmusic.viewModel

import okio.BufferedSink

/**
 * The smallest zip writer a backup needs: whole entries, STORED (no compression), sizes under 4 GB.
 *
 * Kotlin/Native has no java.util.zip and okio only reads zips. A stored entry carries its sizes and
 * CRC in the local header, so `ZipInputStream` on Android and Desktop restores these exactly as it
 * restores their own deflated backups.
 */
internal class StoredZipWriter(
    private val sink: BufferedSink,
) {
    private class Entry(
        val name: ByteArray,
        val crc: Int,
        val size: Int,
        val offset: Int,
    )

    private val entries = mutableListOf<Entry>()
    private var written = 0

    fun add(
        name: String,
        data: ByteArray,
    ) {
        val nameBytes = name.encodeToByteArray()
        val entry = Entry(nameBytes, crc32(data), data.size, written)
        entries += entry
        sink.writeIntLe(LOCAL_HEADER)
        writeCommon(entry)
        sink.write(nameBytes)
        sink.write(data)
        written += 30 + nameBytes.size + data.size
    }

    fun finish() {
        val directoryStart = written
        var directorySize = 0
        for (entry in entries) {
            sink.writeIntLe(CENTRAL_HEADER)
            sink.writeShortLe(VERSION)
            writeCommon(entry)
            sink.writeShortLe(0) // comment length
            sink.writeShortLe(0) // disk number
            sink.writeShortLe(0) // internal attributes
            sink.writeIntLe(0) // external attributes
            sink.writeIntLe(entry.offset)
            sink.write(entry.name)
            directorySize += 46 + entry.name.size
        }
        sink.writeIntLe(END_OF_DIRECTORY)
        sink.writeShortLe(0) // this disk
        sink.writeShortLe(0) // disk with the directory
        sink.writeShortLe(entries.size)
        sink.writeShortLe(entries.size)
        sink.writeIntLe(directorySize)
        sink.writeIntLe(directoryStart)
        sink.writeShortLe(0) // comment length
    }

    // From "version needed" to "extra length": identical in the local and central headers.
    private fun writeCommon(entry: Entry) {
        sink.writeShortLe(VERSION)
        sink.writeShortLe(UTF8_NAMES)
        sink.writeShortLe(0) // STORED
        sink.writeShortLe(0) // time 00:00:00
        sink.writeShortLe(DOS_DATE_1980_01_01)
        sink.writeIntLe(entry.crc)
        sink.writeIntLe(entry.size)
        sink.writeIntLe(entry.size)
        sink.writeShortLe(entry.name.size)
        sink.writeShortLe(0) // extra length
    }

    private companion object {
        const val LOCAL_HEADER = 0x04034b50
        const val CENTRAL_HEADER = 0x02014b50
        const val END_OF_DIRECTORY = 0x06054b50
        const val VERSION = 20
        const val UTF8_NAMES = 0x0800
        const val DOS_DATE_1980_01_01 = (0 shl 9) or (1 shl 5) or 1

        val crcTable =
            IntArray(256) { n ->
                var c = n
                repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
                c
            }

        fun crc32(data: ByteArray): Int {
            var crc = -1
            for (byte in data) crc = crcTable[(crc xor byte.toInt()) and 0xff] xor (crc ushr 8)
            return crc.inv()
        }
    }
}
