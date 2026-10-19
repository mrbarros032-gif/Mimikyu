package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lin0721.linmusic.core.preferences.FullPlayerCard
import com.lin0721.linmusic.core.preferences.FullPlayerCardSetting
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.PlayerBackdropPalette

private const val EDIT_CARDS_KEY = "edit_cards"

private data class VisibleInfoCards(val cards: List<FullPlayerCard>, val allSettled: Boolean)

private fun visibleInfoCards(
    songState: PlayerSongDetailState,
    cardLayout: List<FullPlayerCardSetting>
): VisibleInfoCards {
    val lyrics = songState.lyrics
    val isPureMusic = lyrics.size == 1 && lyrics[0].text == "Instrumental"

    fun slotState(card: FullPlayerCard): Pair<Boolean, Boolean> = when (card) {
        FullPlayerCard.LYRICS -> Pair(!songState.isLyricsLoading, lyrics.isNotEmpty() && !isPureMusic)
        FullPlayerCard.SONG_DETAIL -> Pair(!songState.isSongWikiLoading, songState.songWiki != null)
        FullPlayerCard.MUSIC_MEMORY -> Pair(!songState.isSongWikiLoading, songState.songWiki?.musicMemory != null)
        FullPlayerCard.ABOUT_ARTIST -> Pair(
            !songState.isArtistDetailLoading,
            songState.artistDetail != null || songState.artists.any { it.artistDetail != null }
        )
        FullPlayerCard.ARTIST_ALBUMS -> Pair(!songState.isArtistAlbumsLoading, songState.artistAlbums.isNotEmpty())
        FullPlayerCard.SIMILAR_ARTISTS -> Pair(!songState.isSimilarArtistsLoading, songState.similarArtists.isNotEmpty())
    }

    val cards = mutableListOf<FullPlayerCard>()
    for (setting in cardLayout) {
        if (!setting.visible) continue
        val (settled, ready) = slotState(setting.card)
        if (!settled) return VisibleInfoCards(cards, allSettled = false)
        if (ready) cards += setting.card
    }
    return VisibleInfoCards(cards, allSettled = true)
}

fun LazyListScope.fullPlayerInfoSection(
    songState: PlayerSongDetailState,
    colors: PlayerBackdropPalette,
    currentLyricIndex: Int,
    cardLayout: List<FullPlayerCardSetting>,
    onOpenFullScreenLyrics: () -> Unit,
    onFollowArtistClick: (Long?) -> Unit,
    onArtistClick: (Long) -> Unit,
    onEditCardsClick: () -> Unit,
    onAlbumClick: (Long) -> Unit = {},
    onSelectArtist: (Int) -> Unit = {}
) {
    val visible = visibleInfoCards(songState, cardLayout)
    for (card in visible.cards) {
        item(key = card.key) {
            FullPlayerItemEnterAnimation {
                FullPlayerInfoCard(
                    card = card,
                    songState = songState,
                    colors = colors,
                    currentLyricIndex = currentLyricIndex,
                    onOpenFullScreenLyrics = onOpenFullScreenLyrics,
                    onFollowArtistClick = onFollowArtistClick,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onSelectArtist = onSelectArtist
                )
            }
        }
    }
    if (visible.allSettled) editCardsItem(onEditCardsClick)
}

fun LazyListScope.fullPlayerInfoGrid(
    songState: PlayerSongDetailState,
    colors: PlayerBackdropPalette,
    cardLayout: List<FullPlayerCardSetting>,
    onFollowArtistClick: (Long?) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSelectArtist: (Int) -> Unit,
    onEditCardsClick: () -> Unit
) {
    val visible = visibleInfoCards(songState, cardLayout)
    val cards = visible.cards - FullPlayerCard.LYRICS
    val pairedCards = listOf(FullPlayerCard.ABOUT_ARTIST, FullPlayerCard.SONG_DETAIL)
    val rows = buildList {
        var pairAdded = false
        for (card in cards) {
            if (card in pairedCards) {
                if (!pairAdded) {
                    add(cards.filter { it in pairedCards })
                    pairAdded = true
                }
            } else {
                add(listOf(card))
            }
        }
    }

    for (row in rows) {
        val rowKey = if (row.any { it in pairedCards }) "grid_artist_detail" else "grid_${row.first().key}"
        item(key = rowKey) {
            FullPlayerItemEnterAnimation {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    for (card in row) {
                        Box(modifier = Modifier.weight(1f)) {
                            FullPlayerInfoCard(
                                card = card,
                                songState = songState,
                                colors = colors,
                                currentLyricIndex = -1,
                                onOpenFullScreenLyrics = {},
                                onFollowArtistClick = onFollowArtistClick,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                                onSelectArtist = onSelectArtist
                            )
                        }
                    }
                }
            }
        }
    }
    if (visible.allSettled) editCardsItem(onEditCardsClick)
}

private fun LazyListScope.editCardsItem(onEditCardsClick: () -> Unit) {
    item(key = EDIT_CARDS_KEY) {
        FullPlayerItemEnterAnimation {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MimikyuSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                PlayerCapsuleButton(text = "Edit Cards", onClick = onEditCardsClick)
            }
        }
    }
}

@Composable
private fun FullPlayerInfoCard(
    card: FullPlayerCard,
    songState: PlayerSongDetailState,
    colors: PlayerBackdropPalette,
    currentLyricIndex: Int,
    onOpenFullScreenLyrics: () -> Unit,
    onFollowArtistClick: (Long?) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSelectArtist: (Int) -> Unit
) {
    when (card) {
        FullPlayerCard.LYRICS -> LyricsCard(
            lyrics = songState.lyrics,
            currentIndex = currentLyricIndex,
            isLoading = songState.isLyricsLoading && songState.lyrics.isEmpty(),
            base = colors.base,
            onOpenFullScreen = onOpenFullScreenLyrics
        )
        FullPlayerCard.SONG_DETAIL -> SongDetailCard(
            songWiki = songState.songWiki,
            songDetail = songState.songDetail,
            cardColor = MaterialTheme.colorScheme.surface,
            onAlbumClick = onAlbumClick
        )
        FullPlayerCard.MUSIC_MEMORY -> songState.songWiki?.musicMemory?.let { memory ->
            MusicMemoryCard(
                memory = memory,
                cardColor = MaterialTheme.colorScheme.surface
            )
        }
        FullPlayerCard.ABOUT_ARTIST -> {
            val artistDetail = songState.artistDetail
            if (songState.artists.isNotEmpty()) {
                AboutArtistPagerCard(
                    artists = songState.artists,
                    selectedIndex = songState.selectedArtistIndex,
                    onSelectArtist = onSelectArtist,
                    onFollowArtistClick = { artistId -> onFollowArtistClick(artistId) },
                    cardColor = MaterialTheme.colorScheme.surface,
                    onArtistClick = onArtistClick
                )
            } else if (artistDetail != null) {
                AboutArtistCard(
                    artistDetail = artistDetail,
                    fansCount = songState.artistFansCount,
                    monthlyListeners = songState.artistMonthlyListeners,
                    isFollowed = songState.isArtistFollowed,
                    onFollowClick = { onFollowArtistClick(artistDetail.id) },
                    cardColor = MaterialTheme.colorScheme.surface,
                    onClick = { onArtistClick(artistDetail.id) }
                )
            }
        }
        FullPlayerCard.ARTIST_ALBUMS -> ArtistAlbumsCard(
            albums = songState.artistAlbums,
            artistName = songState.currentArtistItem?.artistName ?: songState.artistDetail?.name,
            cardColor = MaterialTheme.colorScheme.surface,
            onAlbumClick = onAlbumClick
        )
        FullPlayerCard.SIMILAR_ARTISTS -> SimilarArtistsCard(
            artists = songState.similarArtists,
            isLoading = false,
            cardColor = MaterialTheme.colorScheme.surface,
            onArtistClick = onArtistClick
        )
    }
}

@Composable
fun LazyItemScope.FullPlayerItemEnterAnimation(content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = true
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 6 },
        modifier = Modifier.animateItem()
    ) {
        content()
    }
}
