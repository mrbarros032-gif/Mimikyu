package com.lin0721.linmusic.core.player.innertube

import com.metrolist.innertube.models.response.PlayerResponse

data class DualAudioStreams(
    val videoId: String,
    val highQualityUrl: String, // itag 251 (160k Opus) or itag 140 (128k AAC)
    val lowQualityUrl: String,  // itag 249 (50k Opus) or itag 250 (70k Opus)
    val coverUrlHigh: String,   // =w800-h800
    val coverUrlFast: String    // =w300-h300
)

fun extractDualStreams(
    adaptiveFormats: List<PlayerResponse.StreamingData.Format>,
    rawCoverUrl: String,
    videoId: String
): DualAudioStreams? {
    val audioFormats = adaptiveFormats.filter { it.mimeType.contains("audio") && !it.url.isNullOrBlank() }
    if (audioFormats.isEmpty()) return null

    // Prefer itag 140 (AAC) or 251 (Opus) for High Quality
    val highFormat = audioFormats.find { it.itag == 140 || it.itag == 251 }
        ?: audioFormats.maxByOrNull { it.bitrate } ?: audioFormats.first()

    // Prefer itag 250 (70k) or 249 (50k) for Emergency Low Quality
    val lowFormat = audioFormats.find { it.itag == 250 || it.itag == 249 }
        ?: audioFormats.minByOrNull { it.bitrate } ?: highFormat

    val coverHigh = if (rawCoverUrl.contains("=w") || rawCoverUrl.contains("=s")) {
        rawCoverUrl.replace(Regex("=w\\d+-h\\d+.*$"), "=w800-h800-l90-rj")
    } else rawCoverUrl

    val coverFast = if (rawCoverUrl.contains("=w") || rawCoverUrl.contains("=s")) {
        rawCoverUrl.replace(Regex("=w\\d+-h\\d+.*$"), "=w300-h300-l80-rj")
    } else rawCoverUrl

    return DualAudioStreams(
        videoId = videoId,
        highQualityUrl = highFormat.url!!,
        lowQualityUrl = lowFormat.url!!,
        coverUrlHigh = coverHigh,
        coverUrlFast = coverFast
    )
}
