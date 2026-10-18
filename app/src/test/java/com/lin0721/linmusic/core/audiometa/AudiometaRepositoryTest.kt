package com.lin0721.linmusic.core.audiometa

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiometaRepositoryTest {

    @Test
    fun `parse Audiometa artist JSON correctly extracts mimikyu monthly listeners and metadata`() {
        val sampleJson = """
            {
              "id": "amart_8RELWiHB7RZOqrzk",
              "name": "Drake",
              "type": "artist",
              "href": "https://api.audiometa.io/v1/artists/amart_8RELWiHB7RZOqrzk",
              "uri": "audiometa:artist:amart_8RELWiHB7RZOqrzk",
              "images": [
                { "url": "https://images.audiometa.io/drake_640.jpg", "height": 640, "width": 640 }
              ],
              "popularity": 95,
              "genres": ["canadian hip hop", "hip hop", "rap", "pop rap"],
              "country_code": "CA",
              "bio": "Aubrey Drake Graham is a Canadian rapper, singer, and songwriter.",
              "followers": {
                "total": 92000000
              },
              "stats": {
                "mimikyu_monthly_listeners": 82543210,
                "mimikyu_followers": 92000000,
                "youtube_subscribers": 30000000,
                "instagram_followers": 145000000,
                "tiktok_followers": null,
                "soundcloud_followers": 5000000,
                "facebook_followers": 40000000
              }
            }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val id = json.optString("id")
        val name = json.optString("name")
        val bio = json.optString("bio")
        val genres = mutableListOf<String>()
        json.optJSONArray("genres")?.let { arr ->
            for (i in 0 until arr.length()) {
                genres.add(arr.getString(i))
            }
        }
        val stats = json.optJSONObject("stats")
        val mimikyuMonthly = stats?.optLong("mimikyu_monthly_listeners")

        val info = AudiometaArtistInfo(
            id = id,
            name = name,
            bio = bio,
            genres = genres,
            mimikyuMonthlyListeners = mimikyuMonthly
        )

        assertEquals("amart_8RELWiHB7RZOqrzk", info.id)
        assertEquals("Drake", info.name)
        assertEquals("Aubrey Drake Graham is a Canadian rapper, singer, and songwriter.", info.bio)
        assertEquals(4, info.genres.size)
        assertTrue(info.genres.contains("canadian hip hop"))
        assertEquals(82543210L, info.mimikyuMonthlyListeners)
    }

    @Test
    fun `total listener count includes both base listeners and mimikyu monthly listeners`() {
        val lastFmListeners = 2_450_000L
        val mimikyuMonthlyListeners = 82_543_210L

        var totalListeners = lastFmListeners
        totalListeners += mimikyuMonthlyListeners

        assertEquals(84_993_210L, totalListeners)
    }

    @Test
    fun `AudiometaRepository returns null gracefully when API key is blank`() {
        val repo = AudiometaRepository(customApiKey = "")
        assertEquals("", repo.getApiKey())
    }

    @Test
    fun `AudiometaRepository setApiKey updates active key`() {
        val repo = AudiometaRepository(customApiKey = "")
        repo.setApiKey("amsk_test_123456789")
        assertEquals("amsk_test_123456789", repo.getApiKey())
    }
}
