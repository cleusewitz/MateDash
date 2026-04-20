package com.soooool.matedash.data.share

data class TMapShareInfo(
    val destinationName: String,
    val etaText: String,
    val shortUrl: String,
    val raw: String,
)

private val TMAP_URL_REGEX = Regex("""https?://(?:tmap\.life|surl\.tmobi\.co\.kr)/\S+""")

private val ARRIVAL_REGEXES = listOf(
    Regex("""(.+?)(?:에|로|으로)\s*(?:오전|오후)?\s*\d{1,2}\s*(?:시|:)\s*\d{1,2}\s*분?\s*도착\s*예정"""),
    Regex("""(.+?)(?:까지|로|에)\s*약?\s*\d+\s*분"""),
    Regex("""(.+?)(?:에|로|으로)\s*(?:도착|가는\s*길)"""),
)

private val BRACKET_TAG = Regex("""\[[^\]]+\]""")

fun parseTMapShare(raw: String): TMapShareInfo? {
    if (raw.isBlank()) return null

    val shortUrl = TMAP_URL_REGEX.find(raw)?.value ?: return null

    val withoutUrl = raw.replace(shortUrl, " ")
        .replace(BRACKET_TAG, " ")
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .replace(Regex("\\s+"), " ")
        .trim()

    val destination = ARRIVAL_REGEXES.firstNotNullOfOrNull { regex ->
        regex.find(withoutUrl)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    } ?: withoutUrl.takeIf { it.isNotBlank() } ?: return null

    val cleaned = destination
        .replace(BRACKET_TAG, "")
        .removePrefix("\"").removeSuffix("\"")
        .removePrefix("'").removeSuffix("'")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd('.', ',', '·', '•')
        .trim()

    return TMapShareInfo(
        destinationName = cleaned,
        etaText = withoutUrl,
        shortUrl = shortUrl,
        raw = raw,
    )
}
