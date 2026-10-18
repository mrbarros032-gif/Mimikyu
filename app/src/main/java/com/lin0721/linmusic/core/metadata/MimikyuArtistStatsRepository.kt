package com.lin0721.linmusic.core.metadata

import android.content.Context
import com.lin0721.linmusic.core.log.AppLogger
import org.json.JSONArray
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

data class MimikyuArtistStats(
    val rank: Int,
    val name: String,
    val mimikyuId: String,
    val monthlyListeners: Long,
    val peakRank: Int,
    val peakListeners: Long
)

class MimikyuArtistStatsRepository(
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "MimikyuArtistStatsRepo"
        private const val ASSET_FILE = "mimikyu_artist_stats.json"

        private val statsCache = ConcurrentHashMap<String, MimikyuArtistStats>()
        @Volatile
        private var isLoaded = false
        private val lock = Any()
    }

    init {
        loadStats()
    }

    fun loadStats() {
        if (isLoaded) return
        val ctx = context ?: return
        try {
            ctx.assets.open(ASSET_FILE).use { stream ->
                loadFromStream(stream)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load $ASSET_FILE: ${e.message}", e)
        }
    }

    fun loadFromStream(stream: java.io.InputStream) {
        synchronized(lock) {
            if (isLoaded) return
            try {
                val jsonStr = stream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val rank = obj.getInt("rank")
                    val name = obj.getString("name")
                    val mimikyuId = obj.optString("mimikyuId", "")
                    val listeners = obj.getLong("monthlyListeners")
                    val peakRank = obj.optInt("peakRank", 0)
                    val peakListeners = obj.optLong("peakListeners", 0L)
                    val stats = MimikyuArtistStats(
                        rank = rank,
                        name = name,
                        mimikyuId = mimikyuId,
                        monthlyListeners = listeners,
                        peakRank = peakRank,
                        peakListeners = peakListeners
                    )
                    val key = normalizeKey(name)
                    statsCache[key] = stats
                }
                isLoaded = true
                AppLogger.i(TAG, "Successfully loaded ${statsCache.size} Mimikyu artist stats")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to parse Mimikyu stats stream: ${e.message}", e)
            }
        }
    }

    fun getStats(artistName: String): MimikyuArtistStats? {
        if (artistName.isBlank()) return null
        loadStats()

        val directKey = normalizeKey(artistName)
        statsCache[directKey]?.let { return it }

        // Substring / word-boundary match for variant spellings (e.g., "The Weeknd" vs "Weeknd", "Tyler The Creator" vs "Tyler, The Creator")
        return statsCache.entries.firstOrNull { (k, _) ->
            k.length > 3 && (k == directKey || k.contains(directKey) || directKey.contains(k))
        }?.value
    }

    fun getMonthlyListeners(artistName: String): Long {
        return getStats(artistName)?.monthlyListeners ?: 0L
    }

    private fun normalizeKey(name: String): String {
        val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
        val withoutAccents = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents.lowercase()
            .replace("&", "and")
            .replace("[^a-z0-9 ]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }
}
