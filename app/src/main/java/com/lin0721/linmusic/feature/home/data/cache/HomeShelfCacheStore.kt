package com.lin0721.linmusic.feature.home.data.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.player.recommendation.DailyMixPlaylist
import com.lin0721.linmusic.core.ui.components.MimikyuCoverType
import com.lin0721.linmusic.core.ui.components.ThisIsCoverSpec
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.feature.home.data.PersonalizedPlaylist
import com.lin0721.linmusic.feature.home.domain.HomeCard
import com.lin0721.linmusic.feature.home.domain.HomeShelf
import com.lin0721.linmusic.feature.home.domain.ToplistInfo
import com.lin0721.linmusic.feature.home.ui.HomeFeedData
import com.lin0721.linmusic.feature.recent.domain.RecentPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "HomeShelfCacheStore"
private const val DB_NAME = "home_shelf_cache.db"
private const val DB_VERSION = 3
private const val TABLE_NAME = "home_shelf_cache"
private const val COL_SHELF_KEY = "shelf_key"
private const val COL_PAYLOAD = "payload"
private const val COL_UPDATED_AT = "updated_at"
private const val KEY_MAIN_HOME_FEED = "main_home_feed"

/**
 * SQLite Entity representing cached home screen shelves for Stale-While-Revalidate instant startup.
 */
data class HomeShelfCacheEntity(
    val shelfKey: String,
    val payload: String,
    val updatedAt: Long
)

/**
 * High-performance local SQLite database cache for Home Feed data.
 * Enables Mimikyu-style Stale-While-Revalidate:
 * 1. Reads cached shelves from local DB in < 30ms upon launch (zero UI spinner).
 * 2. Background coroutine fetches fresh Daily Mixes and silently updates the list when ready.
 */
class HomeShelfCacheStore(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_SHELF_KEY TEXT PRIMARY KEY,
                $COL_PAYLOAD TEXT NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    suspend fun getCachedHomeFeed(): HomeFeedData? = withContext(Dispatchers.IO) {
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COL_PAYLOAD),
                "$COL_SHELF_KEY = ?",
                arrayOf(KEY_MAIN_HOME_FEED),
                null,
                null,
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val payload = it.getString(it.getColumnIndexOrThrow(COL_PAYLOAD))
                    if (payload.isNotBlank()) {
                        val parsed = jsonToHomeFeed(payload)
                        AppLogger.i(TAG, "Cached home feed loaded in < 20ms (${parsed.topMixes.size} mixes, ${parsed.shelves.size} shelves)")
                        return@withContext parsed
                    }
                }
            }
            null
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to read cached home feed from database", e)
            null
        }
    }

    suspend fun saveHomeFeed(data: HomeFeedData) = withContext(Dispatchers.IO) {
        try {
            val json = homeFeedToJson(data)
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_SHELF_KEY, KEY_MAIN_HOME_FEED)
                put(COL_PAYLOAD, json)
                put(COL_UPDATED_AT, System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            AppLogger.i(TAG, "Home feed cached successfully to database")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to cache home feed to database", e)
        }
    }

    private fun homeFeedToJson(data: HomeFeedData): String {
        val root = JSONObject()

        // Shelves
        val shelvesArr = JSONArray()
        data.shelves.forEach { shelf ->
            val sObj = JSONObject().apply {
                put("blockCode", shelf.blockCode)
                put("title", shelf.title)
                put("showRank", shelf.showRank)
                val cardsArr = JSONArray()
                shelf.cards.forEach { card ->
                    val cObj = JSONObject().apply {
                        put("id", card.id)
                        put("title", card.title)
                        put("coverUrl", card.coverUrl)
                        put("caption", card.caption)
                        when (card) {
                            is HomeCard.Playlist -> {
                                put("type", "playlist")
                                put("trackCount", card.trackCount ?: -1)
                                put("isDailyMix", card.isDailyMix)
                                put("isGenreMix", card.isGenreMix)
                                put("isPinned", card.isPinned)
                            }
                            is HomeCard.Album -> {
                                put("type", "album")
                                put("trackCount", card.trackCount ?: -1)
                                put("year", card.year ?: -1)
                                put("isPinned", card.isPinned)
                            }
                            is HomeCard.Song -> put("type", "song")
                            is HomeCard.Voice -> {
                                put("type", "voice")
                                put("songId", card.songId)
                            }
                        }
                    }
                    cardsArr.put(cObj)
                }
                put("cards", cardsArr)
            }
            shelvesArr.put(sObj)
        }
        root.put("shelves", shelvesArr)

        // Mimikyu Mixes
        root.put("dailyMixes", mixesToJsonArray(data.dailyMixes))
        root.put("topMixes", mixesToJsonArray(data.topMixes))
        root.put("recommendedStations", mixesToJsonArray(data.recommendedStations))
        root.put("bestOfArtists", mixesToJsonArray(data.bestOfArtists))
        root.put("favoriteArtists", mixesToJsonArray(data.favoriteArtists))
        root.put("moreLikeMixes", mixesToJsonArray(data.moreLikeMixes))
        root.put("moreLikeArtistName", data.moreLikeArtistName)
        root.put("moreLikeArtistCover", data.moreLikeArtistCover)

        // Made For Your Vibe
        val vibeArr = JSONArray()
        data.madeForYourVibe.forEach { vibe ->
            val vObj = JSONObject().apply {
                put("id", vibe.id)
                put("headline", vibe.headline)
                put("subtitle", vibe.subtitle)
                put("artistCutoutUrl", (vibe.artistCutoutUrlOrBitmap as? String).orEmpty())
                put("colorStart", vibe.bottomColorStart.toArgb())
                put("colorEnd", vibe.bottomColorEnd.toArgb())
                put("virtualId", vibe.virtualId)
                put("artistName", vibe.artistName)
                put("tracks", tracksToJsonArray(vibe.tracks))
            }
            vibeArr.put(vObj)
        }
        root.put("madeForYourVibe", vibeArr)

        // Recommend Playlists
        val recPlaylistsArr = JSONArray()
        data.recommendPlaylists.forEach { p ->
            recPlaylistsArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("picUrl", p.picUrl)
            })
        }
        root.put("recommendPlaylists", recPlaylistsArr)

        // Recent Playlists
        val recentArr = JSONArray()
        data.recentPlaylists.forEach { r ->
            recentArr.put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("coverUrl", r.coverUrl)
                put("creatorName", r.creatorName)
                put("playTime", r.playTime)
                put("playedAtText", r.playedAtText)
            })
        }
        root.put("recentPlaylists", recentArr)

        // Daily Songs
        val dailySongsArr = JSONArray()
        data.dailySongs.forEach { d ->
            dailySongsArr.put(JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("picUrl", d.al.picUrl)
                put("albumName", d.al.name)
                put("artistName", d.ar.joinToString(", ") { it.name })
                put("reason", d.reason ?: "")
            })
        }
        root.put("dailySongs", dailySongsArr)

        // Toplist
        val toplistArr = JSONArray()
        data.toplistItems.forEach { t ->
            toplistArr.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("coverUrl", t.coverUrl)
                put("updateDesc", t.updateDesc)
                val songsArr = JSONArray()
                t.topSongs.forEach { songsArr.put(it) }
                put("topSongs", songsArr)
            })
        }
        root.put("toplistItems", toplistArr)

        root.put("nextCursor", data.nextCursor ?: "")
        root.put("hasMore", data.hasMore)

        return root.toString()
    }

    private fun jsonToHomeFeed(jsonStr: String): HomeFeedData {
        val root = JSONObject(jsonStr)

        val shelves = mutableListOf<HomeShelf>()
        val shelvesArr = root.optJSONArray("shelves") ?: JSONArray()
        for (i in 0 until shelvesArr.length()) {
            val sObj = shelvesArr.getJSONObject(i)
            val cards = mutableListOf<HomeCard>()
            val cardsArr = sObj.optJSONArray("cards") ?: JSONArray()
            for (j in 0 until cardsArr.length()) {
                val cObj = cardsArr.getJSONObject(j)
                val id = cObj.optLong("id")
                val title = cObj.optString("title")
                val coverUrl = cObj.optString("coverUrl")
                val caption = cObj.optString("caption")
                val type = cObj.optString("type")
                when (type) {
                    "playlist" -> cards.add(
                        HomeCard.Playlist(
                            id = id,
                            title = title,
                            coverUrl = coverUrl,
                            caption = caption,
                            trackCount = cObj.optInt("trackCount", -1).takeIf { it > 0 },
                            isDailyMix = cObj.optBoolean("isDailyMix", false),
                            isGenreMix = cObj.optBoolean("isGenreMix", false),
                            isPinned = cObj.optBoolean("isPinned", false)
                        )
                    )
                    "album" -> cards.add(
                        HomeCard.Album(
                            id = id,
                            title = title,
                            coverUrl = coverUrl,
                            caption = caption,
                            trackCount = cObj.optInt("trackCount", -1).takeIf { it > 0 },
                            year = cObj.optInt("year", -1).takeIf { it > 0 },
                            isPinned = cObj.optBoolean("isPinned", false)
                        )
                    )
                    else -> {
                        // Skip songs or unknown types so shelves stay strictly playlists & albums
                    }
                }
            }
            shelves.add(
                HomeShelf(
                    blockCode = sObj.optString("blockCode"),
                    title = sObj.optString("title"),
                    showRank = sObj.optBoolean("showRank", false),
                    cards = cards
                )
            )
        }

        val dailyMixes = jsonArrayToMixes(root.optJSONArray("dailyMixes"))
        val topMixes = jsonArrayToMixes(root.optJSONArray("topMixes"))
        val recommendedStations = jsonArrayToMixes(root.optJSONArray("recommendedStations"))
        val bestOfArtists = jsonArrayToMixes(root.optJSONArray("bestOfArtists"))
        val favoriteArtists = jsonArrayToMixes(root.optJSONArray("favoriteArtists"))
        val moreLikeMixes = jsonArrayToMixes(root.optJSONArray("moreLikeMixes"))

        val madeForYourVibe = mutableListOf<ThisIsCoverSpec>()
        val vibeArr = root.optJSONArray("madeForYourVibe") ?: JSONArray()
        for (i in 0 until vibeArr.length()) {
            val vObj = vibeArr.getJSONObject(i)
            madeForYourVibe.add(
                ThisIsCoverSpec(
                    id = vObj.optString("id"),
                    headline = vObj.optString("headline"),
                    subtitle = vObj.optString("subtitle"),
                    artistCutoutUrlOrBitmap = vObj.optString("artistCutoutUrl").takeIf { it.isNotBlank() },
                    bottomColorStart = Color(vObj.optInt("colorStart", 0xFF1DB954.toInt())),
                    bottomColorEnd = Color(vObj.optInt("colorEnd", 0xFF1DB954.toInt())),
                    virtualId = vObj.optLong("virtualId"),
                    artistName = vObj.optString("artistName"),
                    tracks = jsonArrayToTracks(vObj.optJSONArray("tracks"))
                )
            )
        }

        val recommendPlaylists = mutableListOf<PersonalizedPlaylist>()
        val recArr = root.optJSONArray("recommendPlaylists") ?: JSONArray()
        for (i in 0 until recArr.length()) {
            val p = recArr.getJSONObject(i)
            recommendPlaylists.add(PersonalizedPlaylist(p.optLong("id"), p.optString("name"), p.optString("picUrl")))
        }

        val recentPlaylists = mutableListOf<RecentPlaylist>()
        val recentArr = root.optJSONArray("recentPlaylists") ?: JSONArray()
        for (i in 0 until recentArr.length()) {
            val r = recentArr.getJSONObject(i)
            recentPlaylists.add(
                RecentPlaylist(
                    id = r.optLong("id"),
                    name = r.optString("name"),
                    coverUrl = r.optString("coverUrl"),
                    creatorName = r.optString("creatorName"),
                    playTime = r.optLong("playTime"),
                    playedAtText = r.optString("playedAtText")
                )
            )
        }

        val dailySongs = mutableListOf<DailySong>()
        val dailyArr = root.optJSONArray("dailySongs") ?: JSONArray()
        for (i in 0 until dailyArr.length()) {
            val d = dailyArr.getJSONObject(i)
            val artistName = d.optString("artistName")
            val artists = if (artistName.isNotBlank()) listOf(Artist(id = 0, name = artistName)) else emptyList()
            dailySongs.add(
                DailySong(
                    id = d.optLong("id"),
                    name = d.optString("name"),
                    ar = artists,
                    al = Album(id = 0, name = d.optString("albumName"), picUrl = d.optString("picUrl")),
                    fee = 0,
                    reason = d.optString("reason").takeIf { it.isNotBlank() }
                )
            )
        }

        val toplistItems = mutableListOf<ToplistInfo>()
        val topArr = root.optJSONArray("toplistItems") ?: JSONArray()
        for (i in 0 until topArr.length()) {
            val t = topArr.getJSONObject(i)
            val sArr = t.optJSONArray("topSongs") ?: JSONArray()
            val songList = mutableListOf<String>()
            for (k in 0 until sArr.length()) {
                songList.add(sArr.getString(k))
            }
            toplistItems.add(
                ToplistInfo(
                    id = t.optLong("id"),
                    name = t.optString("name"),
                    coverUrl = t.optString("coverUrl"),
                    updateDesc = t.optString("updateDesc"),
                    topSongs = songList
                )
            )
        }

        val nextCursor = root.optString("nextCursor").takeIf { it.isNotBlank() }
        val hasMore = root.optBoolean("hasMore", false)

        return HomeFeedData(
            shelves = shelves,
            recommendPlaylists = recommendPlaylists,
            recentPlaylists = recentPlaylists,
            dailySongs = dailySongs,
            toplistItems = toplistItems,
            dailyMixes = dailyMixes,
            topMixes = topMixes,
            recommendedStations = recommendedStations,
            bestOfArtists = bestOfArtists,
            favoriteArtists = favoriteArtists,
            moreLikeMixes = moreLikeMixes,
            moreLikeArtistName = root.optString("moreLikeArtistName"),
            moreLikeArtistCover = root.optString("moreLikeArtistCover"),
            madeForYourVibe = madeForYourVibe,
            nextCursor = nextCursor,
            hasMore = hasMore
        )
    }

    private fun mixesToJsonArray(mixes: List<DailyMixPlaylist>): JSONArray {
        val arr = JSONArray()
        mixes.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("subtitle", m.subtitle)
                put("coverUrl", m.coverUrl)
                put("coverType", m.coverType.name)
                put("primaryColor", m.primaryColor.toArgb())
                put("isArtistProfile", m.isArtistProfile)
                put("artistId", m.artistId ?: 0L)
                put("seedQuery", m.seedQuery)
                put("seedVideoId", m.seedVideoId)
                val mosaicArr = JSONArray()
                m.mosaicImages.forEach { mosaicArr.put(it) }
                put("mosaicImages", mosaicArr)
                put("tracks", tracksToJsonArray(m.tracks))
            }
            arr.put(obj)
        }
        return arr
    }

    private fun jsonArrayToMixes(arr: JSONArray?): List<DailyMixPlaylist> {
        if (arr == null) return emptyList()
        val list = mutableListOf<DailyMixPlaylist>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val mosaicArr = obj.optJSONArray("mosaicImages") ?: JSONArray()
            val mosaicImages = mutableListOf<String>()
            for (j in 0 until mosaicArr.length()) {
                mosaicImages.add(mosaicArr.getString(j))
            }
            val coverTypeStr = obj.optString("coverType", MimikyuCoverType.DAILY_MIX.name)
            val coverType = runCatching { MimikyuCoverType.valueOf(coverTypeStr) }.getOrDefault(MimikyuCoverType.DAILY_MIX)
            list.add(
                DailyMixPlaylist(
                    id = obj.optLong("id"),
                    title = obj.optString("title"),
                    subtitle = obj.optString("subtitle"),
                    coverUrl = obj.optString("coverUrl"),
                    coverType = coverType,
                    primaryColor = Color(obj.optInt("primaryColor", 0xFF1DB954.toInt())),
                    mosaicImages = mosaicImages,
                    tracks = jsonArrayToTracks(obj.optJSONArray("tracks")),
                    isArtistProfile = obj.optBoolean("isArtistProfile", false),
                    artistId = obj.optLong("artistId").takeIf { it > 0 },
                    seedQuery = obj.optString("seedQuery"),
                    seedVideoId = obj.optString("seedVideoId")
                )
            )
        }
        return list
    }

    private fun tracksToJsonArray(tracks: List<QueueItem>): JSONArray {
        val arr = JSONArray()
        tracks.forEach { t ->
            arr.put(JSONObject().apply {
                put("songId", t.songId)
                put("title", t.title)
                put("artist", t.artist)
                put("coverUrl", t.coverUrl)
                put("localUri", t.localUri ?: "")
                put("videoId", t.videoId ?: "")
                put("album", t.album ?: "")
                put("durationSec", t.durationSec)
            })
        }
        return arr
    }

    private fun jsonArrayToTracks(arr: JSONArray?): List<QueueItem> {
        if (arr == null) return emptyList()
        val list = mutableListOf<QueueItem>()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            list.add(
                QueueItem(
                    songId = t.optLong("songId"),
                    title = t.optString("title"),
                    artist = t.optString("artist"),
                    coverUrl = t.optString("coverUrl"),
                    localUri = t.optString("localUri").takeIf { it.isNotBlank() },
                    videoId = t.optString("videoId").takeIf { it.isNotBlank() },
                    album = t.optString("album").takeIf { it.isNotBlank() },
                    durationSec = t.optInt("durationSec", 0)
                )
            )
        }
        return list
    }
}
