package com.lin0721.linmusic.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class MimikyuArtistStatsRepositoryTest {

    companion object {
        private lateinit var repo: MimikyuArtistStatsRepository

        @BeforeClass
        @JvmStatic
        fun setUp() {
            // Find asset file in project structure
            val candidates = listOf(
                File("src/main/assets/mimikyu_artist_stats.json"),
                File("app/src/main/assets/mimikyu_artist_stats.json")
            )
            val assetFile = candidates.firstOrNull { it.exists() }
                ?: error("Could not find mimikyu_artist_stats.json")

            repo = MimikyuArtistStatsRepository(context = null)
            assetFile.inputStream().use { stream ->
                repo.loadFromStream(stream)
            }
        }
    }

    @Test
    fun testBrunoMars_matchesExactStats() {
        val stats = repo.getStats("Bruno Mars")
        assertNotNull(stats)
        assertEquals(1, stats?.rank)
        assertEquals("Bruno Mars", stats?.name)
        assertEquals("0du5cEVh5yTK9QJze8zA0C", stats?.mimikyuId)
        assertTrue(stats?.monthlyListeners ?: 0L >= 120_000_000L)
    }

    @Test
    fun testCaseAndPunctuationInsensitive() {
        val s1 = repo.getStats("bruno mars")
        val s2 = repo.getStats("BRUNO MARS")
        assertNotNull(s1)
        assertEquals(s1?.monthlyListeners, s2?.monthlyListeners)

        val weeknd = repo.getStats("The Weeknd")
        val weekndLower = repo.getStats("the weeknd")
        assertNotNull(weeknd)
        assertEquals(weeknd?.mimikyuId, weekndLower?.mimikyuId)
    }

    @Test
    fun testTaylorSwiftAndDrake() {
        val taylor = repo.getStats("Taylor Swift")
        assertNotNull(taylor)
        assertTrue(taylor?.monthlyListeners ?: 0L >= 100_000_000L)

        val drake = repo.getStats("Drake")
        assertNotNull(drake)
        assertTrue(drake?.monthlyListeners ?: 0L >= 90_000_000L)
    }
}
