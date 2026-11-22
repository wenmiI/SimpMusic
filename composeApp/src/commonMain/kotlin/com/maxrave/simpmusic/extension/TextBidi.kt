package com.maxrave.simpmusic.extension

/**
 * The two questions lyrics ask of `java.text.Bidi`, answered without it, since it is JVM-only.
 *
 * The base direction comes from the first strong character, as the Unicode Bidirectional
 * Algorithm's rules P2/P3 decide it, and [fallbackLeftToRight] is used when there is none — the
 * same contract as `Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT` / `DIRECTION_DEFAULT_RIGHT_TO_LEFT`.
 * Strong classes are approximated by script block: every right-to-left script (Hebrew, Arabic,
 * Syriac, Thaana, N'Ko, Samaritan, Mandaic and their presentation forms, plus the historic RTL
 * scripts in the SMP) is R/AL, and any other letter is L. That is what lyrics contain.
 */
class TextBidi(
    text: String,
    fallbackLeftToRight: Boolean = true,
) {
    private val hasRtl: Boolean
    private val baseLeftToRight: Boolean

    init {
        var first: Boolean? = null
        var anyRtl = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val codePoint =
                if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                    i++
                    0x10000 + ((c.code - 0xD800) shl 10) + (text[i].code - 0xDC00)
                } else {
                    c.code
                }
            i++
            val rtl = isRtlCodePoint(codePoint)
            if (rtl) anyRtl = true
            if (first == null) {
                if (rtl) {
                    first = true
                } else if (codePoint < 0x10000 && codePoint.toChar().isLetter()) {
                    first = false
                }
            }
            if (first != null && anyRtl) break
        }
        hasRtl = anyRtl
        baseLeftToRight = first?.not() ?: fallbackLeftToRight
    }

    fun baseIsLeftToRight(): Boolean = baseLeftToRight

    /** True only when nothing in the text runs right to left, as `Bidi.isLeftToRight` reports. */
    val isLeftToRight: Boolean
        get() = baseLeftToRight && !hasRtl

    private companion object {
        fun isRtlCodePoint(cp: Int): Boolean =
            cp in 0x0590..0x08FF ||
                cp in 0xFB1D..0xFDFF ||
                cp in 0xFE70..0xFEFF ||
                cp in 0x10800..0x10FFF ||
                cp in 0x1E800..0x1EFFF
    }
}
