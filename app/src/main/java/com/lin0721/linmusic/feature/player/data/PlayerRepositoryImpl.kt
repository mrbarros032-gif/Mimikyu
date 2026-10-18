package com.lin0721.linmusic.feature.player.data

import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.network.apiFlow
import com.lin0721.linmusic.core.network.mapToAppError
import com.lin0721.linmusic.feature.player.domain.SongMusicMemory
import com.lin0721.linmusic.feature.player.domain.SongWikiCreatorRole
import com.lin0721.linmusic.feature.player.domain.SongWikiData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

private const val TAG = "PlayerRepositoryImpl"

class PlayerRepositoryImpl(
    private val apiService: PlayerApi
) : PlayerRepository {

    override fun getSongDetail(songId: Long): Flow<Result<Track>> = flow {
        val registeredTrack = YouTubeIdRegistry.getTrack(songId)
        if (registeredTrack != null) {
            emit(Result.success(registeredTrack))
            return@flow
        }
        if (YouTubeIdRegistry.isYouTubeTrack(songId)) {
            val title = YouTubeIdRegistry.getTitle(songId)
            val artist = YouTubeIdRegistry.getArtist(songId)
            val album = YouTubeIdRegistry.getAlbumName(songId)
            val coverUrl = YouTubeIdRegistry.getCoverUrl(songId)
            val artists = YouTubeIdRegistry.getArtists(songId)
            val durationSec = YouTubeIdRegistry.getDurationSec(songId)
            val validArtists = if (artists.isNotEmpty()) {
                artists
            } else if (artist.isNotBlank()) {
                val aId = YouTubeIdRegistry.registerArtistByName(artist)
                listOf(Artist(id = aId, name = artist))
            } else emptyList()

            val track = Track(
                id = songId,
                name = title,
                ar = validArtists,
                al = Album(
                    id = if (album.isNotBlank()) (album.hashCode().toLong() and 0x7FFFFFFFL) else 0L,
                    name = album,
                    picUrl = coverUrl
                ),
                dt = durationSec.toLong() * 1000L
            )
            emit(Result.success(track))
            return@flow
        }
        val detailResult = try {
            val c = """[{"id":$songId}]"""
            val resp = apiService.getSongDetail(SongDetailRequest(c = c))
            if (resp.isSuccess && resp.songs.isNotEmpty()) {
                Result.success(resp.songs[0])
            } else {
                Result.failure(Exception("Song detail not found"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(mapToAppError(e))
        }
        emit(detailResult)
    }

    override fun getSongDetails(songIds: List<Long>): Flow<Result<List<Track>>> = apiFlow(
        request = {
            val c = songIds.joinToString(prefix = "[", postfix = "]") { """{"id":$it}""" }
            apiService.getSongDetail(SongDetailRequest(c = c))
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.songs }
    )

    override fun getChorusStartTime(songId: Long): Flow<Result<Long?>> = flow {
        if (YouTubeIdRegistry.isYouTubeTrack(songId)) {
            emit(Result.success(null))
            return@flow
        }
        val time = try {
            val response = apiService.getSongChorus(SongChorusRequest(ids = "[$songId]"))
            if (response.isSuccess) {
                response.chorus
                    .filter { it.id == songId && it.startTime > 0L }
                    .minOfOrNull { it.startTime }
            } else null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
        emit(Result.success(time))
    }

    // of SongsDetails and
    override fun getSongWiki(songId: Long): Flow<Result<SongWikiData>> = flow {
        if (YouTubeIdRegistry.isYouTubeTrack(songId)) {
            val album = YouTubeIdRegistry.getAlbumName(songId)
            val artist = YouTubeIdRegistry.getArtist(songId)
            val wiki = SongWikiData(
                album = album,
                creators = artist,
                creatorRoles = if (artist.isNotBlank()) {
                    listOf(SongWikiCreatorRole(roleName = "Artist", artistNames = artist.split(",").map { it.trim() }))
                } else emptyList()
            )
            emit(Result.success(wiki))
            return@flow
        }
        coroutineScope {

            val detailDeferred = async {
                runCatching {
                    val c = """[{"id":$songId}]"""
                    apiService.getSongDetail(SongDetailRequest(c = c))
                }
            }
            val wikiDeferred = async {
                runCatching {
                    apiService.getSongWikiSummary(SongWikiSummaryRequest(songId = songId))
                }
            }
            val creatorsDeferred = async {
                runCatching {
                    apiService.getSongCreators(SongCreatorsRequest(songId = songId))
                }
            }

            val detailOutcome = detailDeferred.await()
            val wikiOutcome = wikiDeferred.await()
            val creatorsOutcome = creatorsDeferred.await()
            detailOutcome.exceptionOrNull()?.let { AppLogger.w(TAG, "getSongWiki song details sub-request failed: songId=$songId", it) }
            wikiOutcome.exceptionOrNull()?.let { AppLogger.w(TAG, "getSongWiki wiki summary sub-request failed: songId=$songId", it) }
            creatorsOutcome.exceptionOrNull()?.let { AppLogger.w(TAG, "getSongWiki credits sub-request failed: songId=$songId", it) }

            val detailResult = detailOutcome.getOrNull()
            val wikiResult = wikiOutcome.getOrNull()
            val creatorsResult = creatorsOutcome.getOrNull()

            // ：Albums and
            val albumName = detailResult?.songs?.firstOrNull()?.al?.name ?: ""
            val publishTime = detailResult?.songs?.firstOrNull()?.publishTime ?: 0L
            val publishDateStr = if (publishTime > 0) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(publishTime))
            } else {
                ""
            }

            var style = ""
            var language = ""
            var bpm = ""
            var entertainment = ""
            var awards = emptyList<String>()
            var awardTotal = 0
            var musicMemory: SongMusicMemory? = null

            // of  Block
            wikiResult?.data?.blocks?.forEach { block ->
                if (block.code == "SONG_PLAY_ABOUT_SONG_BASIC") {
                    block.creatives.forEach { creative ->
                        when (creative.creativeType) {
                            "songTag" -> {
                                style = creative.resources.mapNotNull { it.uiElement?.mainTitle?.title }
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" / ")
                            }
                            "language" -> {
                                language = creative.uiElement?.textLinks?.firstOrNull()?.text ?: ""
                            }
                            "bpm" -> {
                                bpm = creative.uiElement?.textLinks?.firstOrNull()?.text ?: ""
                            }
                            "entertainment" -> {
                                entertainment = creative.resources.mapNotNull { it.uiElement?.mainTitle?.title }
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" / ")
                            }
                            "songAward" -> {
                                awards = creative.resources.mapNotNull { it.uiElement?.mainTitle?.title }
                                    .filter { it.isNotEmpty() }
                                awardTotal = creative.uiElement?.buttons?.firstOrNull()?.text
                                    ?.filter { it.isDigit() }
                                    ?.toIntOrNull() ?: 0
                            }
                        }
                    }
                } else if (block.code == "SONG_PLAY_ABOUT_MUSIC_MEMORY") {
                    musicMemory = parseMusicMemory(block)
                }
            }

            // Producer：，"Producer"Details；
            val creatorRoles = (creatorsResult?.data?.songCreatorsRoleVos ?: emptyList())
                .mapNotNull { role ->
                    val artists = role.creatorMetaVOS.map { it.artistName }.filter { it.isNotEmpty() }
                    if (artists.isNotEmpty()) SongWikiCreatorRole(role.roleName, artists) else null
                }
            val creatorsStr = creatorRoles.joinToString(" / ") { "${it.roleName} ${it.artistNames.joinToString(" ")}" }

            emit(Result.success(
                SongWikiData(
                    style = style,
                    album = albumName,
                    language = language,
                    publishTime = publishDateStr,
                    bpm = bpm,
                    creators = creatorsStr,
                    creatorRoles = creatorRoles,
                    entertainment = entertainment,
                    awards = awards,
                    awardTotal = awardTotal,
                    musicMemory = musicMemory
                )
            ))
        }
    }.catch { e ->
        AppLogger.e(TAG, "getSongWiki request error: songId=$songId", e)
        emit(Result.failure(mapToAppError(e)))
    }
}

// and PlayBack null，
internal fun parseMusicMemory(block: SongWikiBlock): SongMusicMemory? {
    val resources = block.creatives.flatMap { it.resources }
    val firstListen = resources.firstOrNull { it.resourceType == "FIRST_LISTEN" }
        ?.resourceExt?.musicFirstListenDto
    val totalPlay = resources.firstOrNull { it.resourceType == "TOTAL_PLAY" }
        ?.resourceExt?.musicTotalPlayDto
    val date = firstListen?.date?.trim().orEmpty()
    val playCount = totalPlay?.playCount?.coerceAtLeast(0) ?: 0
    if (date.isEmpty() && playCount == 0) return null
    return SongMusicMemory(
        firstListenDate = date,
        season = firstListen?.season?.trim().orEmpty(),
        period = firstListen?.period?.trim().orEmpty(),
        playCount = playCount,
        playCountText = totalPlay?.text?.trim().orEmpty()
    )
}
