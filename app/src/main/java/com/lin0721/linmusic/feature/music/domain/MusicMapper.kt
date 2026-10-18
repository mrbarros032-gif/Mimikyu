package com.lin0721.linmusic.feature.music.domain

import com.lin0721.linmusic.feature.music.data.StyleArtistDto
import com.lin0721.linmusic.feature.music.data.StyleHeadDto
import com.lin0721.linmusic.feature.music.data.StylePlaylistDto
import com.lin0721.linmusic.feature.music.data.StylePreferenceData
import com.lin0721.linmusic.feature.music.data.StyleTagDto
import com.lin0721.linmusic.feature.music.data.StyleTagPortraitDto

// of  ${tagName}， pattern 。
// ： JVM  of  }，Android  PatternSyntaxException
private val PORTRAIT_PLACEHOLDER = Regex("""\$\{(\w+)\}""")

// ，Genres
private val HEX_COLOR = Regex("""^[0-9a-fA-F]{6}$""")

// of ，。 NUL ，
// of ； Char 。
// ： JVM  NUL  of  pattern，Android  PatternSyntaxException。
private val EMPTY_SLOT_CHAR = Char(0)
private val EMPTY_SLOT = EMPTY_SLOT_CHAR.toString()
private const val SEPARATORS = "、，,"

// Genres。 id  of ，
fun List<StyleTagDto>.toMusicStyles(): List<MusicStyle> = mapNotNull { it.toMusicStyleOrNull() }

private fun StyleTagDto.toMusicStyleOrNull(): MusicStyle? {
    if (tagId <= 0 || tagName.isBlank()) return null
    return MusicStyle(
        id = tagId,
        name = tagName,
        enName = enName.orEmpty(),
        colorHex = colorDeep.normalizeHex(),
        children = childrenTags.mapNotNull { it.toMusicStyleOrNull() }
    )
}

// ， of  tags ， UI
fun StylePreferenceData.toStylePreferences(): List<StylePreference> {
    val colorById = tags.associate { it.tagId to it.colorDeep.normalizeHex() }
    return tagPreferenceVos.mapNotNull { dto ->
        if (dto.tagId <= 0 || dto.tagName.isBlank()) return@mapNotNull null
        StylePreference(
            id = dto.tagId,
            name = dto.tagName,
            // ， 0
            ratio = dto.ratio.trim().toIntOrNull() ?: 0,
            colorHex = colorById[dto.tagId]
        )
    }
}

fun StyleHeadDto.toStyleHead(): StyleHead = StyleHead(
    id = tagId,
    name = name,
    enName = enName.orEmpty(),
    // and ，
    desc = desc.orEmpty().replace('　', ' ').lines().joinToString(" ") { it.trim() }.trim(),
    coverUrl = cover.firstOrNull()?.takeIf { it.isNotBlank() },
    colorHex = colorDeep.normalizeHex(),
    songNum = songNum.orEmpty(),
    artistNum = artistNum.orEmpty(),
    favouriteSong = favouriteSong?.favouriteSong,
    portrait = tagPortrait?.toPortraitOrNull()
)

// 23 Genres，。 of ，
// of ，「、、、Genres」。
private fun StyleTagPortraitDto.toPortraitOrNull(): StylePortrait? {
    val template = templateContent?.takeIf { it.isNotBlank() } ?: return null

    val marked = PORTRAIT_PLACEHOLDER.replace(template) { match ->
        pattern[match.groupValues[1]]?.text?.takeIf { it.isNotBlank() } ?: EMPTY_SLOT
    }
    val cleaned = marked.stripEmptySlots()
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")

    return if (cleaned.isBlank()) null else StylePortrait(cleaned, dataTip.orEmpty())
}

// ：，。
// 「A、、B」「AB」， of 。
private fun String.stripEmptySlots(): String {
    if (indexOf(EMPTY_SLOT_CHAR) < 0) return this

    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        if (char != EMPTY_SLOT_CHAR) {
            result.append(char)
            index++
            continue
        }
        if (result.isNotEmpty() && result.last() in SEPARATORS) {
            result.deleteCharAt(result.length - 1)
        } else if (index + 1 < length && this[index + 1] in SEPARATORS) {
            index++
        }
        index++
    }
    return result.toString()
}

fun List<StylePlaylistDto>.toStylePlaylistItems(): List<StylePlaylistItem> = mapNotNull { dto ->
    if (dto.id <= 0 || dto.name.isBlank()) return@mapNotNull null
    val cover = dto.cover?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    StylePlaylistItem(dto.id, dto.name, cover, dto.playCount)
}

// Artists and  1:1 ， 1:1 ——
fun List<StyleArtistDto>.toStyleArtistItems(): List<StyleArtistItem> = mapNotNull { dto ->
    if (dto.id <= 0 || dto.name.isBlank()) return@mapNotNull null
    val pic = dto.img1v1Url?.takeIf { it.isNotBlank() }
        ?: dto.picUrl?.takeIf { it.isNotBlank() }
        ?: return@mapNotNull null
    StyleArtistItem(dto.id, dto.name, pic, dto.musicSize)
}

private fun String?.normalizeHex(): String? = this?.trim()?.takeIf { HEX_COLOR.matches(it) }
