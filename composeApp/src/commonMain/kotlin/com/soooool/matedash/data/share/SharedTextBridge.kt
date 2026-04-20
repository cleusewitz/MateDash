package com.soooool.matedash.data.share

/** App Group UserDefaults에서 최근 공유된 원문을 읽는다. iOS 전용. */
expect fun readSharedText(): String?

/** 공유 원문 초기화 */
expect fun clearSharedText()

/** 테스트용: 임의 텍스트를 공유 수신한 것으로 기록 */
expect fun writeTestSharedText(text: String)

data class SharedPlace(
    val source: String,
    val name: String,
    val address: String,
    val url: String,
    val raw: String,
)

/**
 * 네이버지도/카카오맵 등 공유 문자열에서 장소 정보를 추출한다.
 *
 * 예:
 *   [네이버지도]
 *   하나로마트 군자농협대부점
 *   경기 안산시 단원구 대부중앙로 142
 *   https://naver.me/I5caIqaF
 */
fun parseSharedPlace(raw: String): SharedPlace? {
    if (raw.isBlank()) return null

    val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return null

    val urlRegex = Regex("""https?://\S+""")
    val url = lines.firstNotNullOfOrNull { urlRegex.find(it)?.value } ?: ""

    val bracketRegex = Regex("""^\[([^\]]+)\]""")
    val source = lines.firstNotNullOfOrNull { bracketRegex.find(it)?.groupValues?.get(1) } ?: ""

    val nonMeta = lines.filter { line ->
        !bracketRegex.containsMatchIn(line) && urlRegex.find(line)?.value != line
    }

    val name = nonMeta.getOrNull(0) ?: ""
    val address = nonMeta.getOrNull(1) ?: ""

    if (name.isBlank() && address.isBlank() && url.isBlank()) return null

    return SharedPlace(
        source = source,
        name = name,
        address = address,
        url = url,
        raw = raw,
    )
}
