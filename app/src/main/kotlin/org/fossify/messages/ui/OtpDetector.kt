package org.fossify.messages.ui

object OtpDetector {
    private val strongContext = Regex(
        """(?i)\b(?:otp|one[- ]?time|verification|verify|passcode|2fa|security\s+code)\b"""
    )
    private val labeled = Regex(
        """(?i)\b(?:otp|one[- ]?time\s+(?:password|code)|verification\s+code|verify(?:\s+code)?|passcode|security\s+code|authorization\s+code|code|pin|token|password)\s*(?:is|:|=|-)?\s*([A-Z0-9][A-Z0-9\s-]{3,15})\b"""
    )
    private val sixDigits = Regex("""(?<!\d)\d{6}(?!\d)""")
    private val fourToEightDigits = Regex("""(?<!\d)\d{4,8}(?!\d)""")
    private val alphaNumeric = Regex("""(?<![A-Za-z0-9])[A-Za-z0-9]{4,12}(?![A-Za-z0-9])""")

    fun findRanges(text: String): List<IntRange> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<IntRange>()

        fun addRange(range: IntRange) {
            val raw = text.substring(range)
            val normalized = raw.replace(Regex("[^A-Za-z0-9]"), "")
            if (normalized.length !in 4..12) return
            if (normalized.toIntOrNull() in 1900..2099) return
            result += range
        }

        labeled.findAll(text).forEach { match ->
            val group = match.groups[1] ?: return@forEach
            val value = group.value.trim()
            val offset = value.indexOfFirst { it.isLetterOrDigit() }
            if (offset >= 0) {
                addRange((group.range.first + offset)..group.range.last)
            }
        }

        if (strongContext.containsMatchIn(text)) {
            sixDigits.findAll(text).forEach { addRange(it.range) }
            fourToEightDigits.findAll(text).forEach { addRange(it.range) }
            alphaNumeric.findAll(text).forEach {
                if (it.value.any(Char::isDigit) && it.value.any(Char::isLetter)) addRange(it.range)
            }
        }

        return result.distinct().sortedBy { it.first }
    }
}
