package com.lin0721.linmusic.feature.home.domain

import com.lin0721.linmusic.feature.home.data.HomeBlockDto
import com.lin0721.linmusic.feature.home.data.HomeBlockPageData
import com.lin0721.linmusic.feature.home.data.HomeResourceDto
import com.lin0721.linmusic.feature.home.data.HomeUiElementDto

private const val BLOCK_BANNER = "HOMEPAGE_BANNER"
private const val BLOCK_DRAGON_BALL = "HOMEPAGE_BLOCK_OLD_DRAGON_BALL"
private const val BLOCK_TOPLIST = "HOMEPAGE_BLOCK_TOPLIST"

// orpheus://playlist/123  and  orpheus://album/123
private val PLAYLIST_ACTION = Regex("""orpheus://playlist/(\d+)""")
private val ALBUM_ACTION = Regex("""orpheus://album/(\d+)""")

// of 。
// of ——， of 。
fun HomeBlockPageData.toHomeBlockPage(): HomeBlockPage {
    val shelves = blocks.mapNotNull { it.toShelfOrNull() }

    return HomeBlockPage(
        shelves = shelves,
        nextCursor = cursor?.takeIf { it.isNotBlank() && it != "null" },
        hasMore = hasMore
    )
}

// and ： of （、、）
private fun HomeBlockDto.toShelfOrNull(): HomeShelf? {
    if (blockCode == BLOCK_BANNER || blockCode == BLOCK_DRAGON_BALL) return null

    val element = uiElement ?: return null
    val title = element.resolveTitle()
    if (title.isBlank()) return null

    val cards = creatives.flatMap { it.resources }.mapNotNull { it.toCardOrNull() }
    if (cards.isEmpty()) return null

    return HomeShelf(
        blockCode = blockCode,
        title = title,
        showRank = blockCode == BLOCK_TOPLIST,
        cards = cards
    )
}

// ： subTitle，Follow mainTitle
private fun HomeUiElementDto.resolveTitle(): String =
    mainTitle?.title?.takeIf { it.isNotBlank() }
        ?: subTitle?.title?.takeIf { it.isNotBlank() }
        ?: ""

private fun HomeResourceDto.toCardOrNull(): HomeCard? {
    val ui = uiElement ?: return null
    val name = ui.mainTitle?.title?.takeIf { it.isNotBlank() } ?: return null
    val cover = ui.image?.imageUrl?.takeIf { it.isNotBlank() } ?: return null
    val caption = ui.resolveCaption()

    return when (resourceType.lowercase()) {
        "list" -> {
            val id = PLAYLIST_ACTION.find(action.orEmpty())?.groupValues?.get(1)?.toLongOrNull()
                ?: resourceId?.toLongOrNull()
                ?: return null
            HomeCard.Playlist(id, name, cover, caption)
        }
        // of  ALBUM  action DetailsAlbums，；
        // Albums openurl ，
        "album", "digitalalbum" -> {
            val id = ALBUM_ACTION.find(action.orEmpty())?.groupValues?.get(1)?.toLongOrNull()
                ?: return null
            HomeCard.Album(id, name, cover, caption)
        }
        "song" -> {
            val id = resourceId?.toLongOrNull() ?: return null
            HomeCard.Song(id, name, cover, caption)
        }
        // 。 resourceExtInfo ， of  mainSong ，
        // Details； mainSong  of
        "voice" -> {
            val program = resourceExtInfo?.djProgram ?: return null
            val songId = program.mainSong?.id?.takeIf { it > 0 } ?: return null
            val programId = program.id.takeIf { it > 0 } ?: resourceId?.toLongOrNull() ?: return null
            HomeCard.Voice(programId, songId, name, cover, caption)
        }
        else -> null
    }
}


private fun HomeUiElementDto.resolveCaption(): String {
    labelTexts.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { return it.joinToString(" · ") }
    labelText?.text?.takeIf { it.isNotBlank() }?.let { return it }
    subTitle?.title?.takeIf { it.isNotBlank() }?.let { return it }
    return description?.takeIf { it.isNotBlank() }.orEmpty()
}
