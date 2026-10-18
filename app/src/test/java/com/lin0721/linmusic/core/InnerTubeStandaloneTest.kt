package com.lin0721.linmusic.core

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YouTubeClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InnerTubeStandaloneTest {

    @Test
    fun testYouTubeSearchAndStreamResolution() {
        runBlocking {
            // 1. Search YouTube Music catalog
            val searchResult = YouTube.search("Coldplay Yellow", YouTube.SearchFilter.FILTER_SONG)
            assertTrue("Expected searchResult to be successful", searchResult.isSuccess)
            val searchPage = searchResult.getOrNull()
            assertNotNull("Expected non-null searchPage", searchPage)

            val firstSong = searchPage?.items?.filterIsInstance<SongItem>()?.firstOrNull()
            assertNotNull("Expected at least one song result", firstSong)
            println("Found song: ${firstSong?.title} by ${firstSong?.artists?.joinToString { it.name }} (id: ${firstSong?.id})")

            val songId = firstSong!!.id
            val playerResult = YouTube.player(videoId = songId, client = YouTubeClient.IOS)
            assertTrue("Expected IOS player response to succeed", playerResult.isSuccess)
            val response = playerResult.getOrNull()
            assertNotNull("Expected response", response)

            println("IOS PlayabilityStatus: ${response?.playabilityStatus?.status}")
            println("IOS regular formats: ${response?.streamingData?.formats?.size}")
            response?.streamingData?.formats?.forEach {
                println("regular format itag=${it.itag} mime=${it.mimeType} url=${it.url?.take(40)}")
            }

            println("IOS adaptive formats: ${response?.streamingData?.adaptiveFormats?.size}")
            response?.streamingData?.adaptiveFormats?.forEach {
                println("adaptive format itag=${it.itag} mime=${it.mimeType} url=${it.url?.take(40)}")
            }
        }
    }

    @Test
    fun testYouTubeArtistDetail() {
        runBlocking {
            val artistResult = YouTube.artist("UC2XdaAVUannpujzv32jcouQ")
            println("artistResult isSuccess: ${artistResult.isSuccess}")
            if (artistResult.isFailure) {
                artistResult.exceptionOrNull()?.printStackTrace()
            }
            val page = artistResult.getOrNull()
            println("page title: ${page?.artist?.title}")
            println("page thumbnail: ${page?.artist?.thumbnail}")
            println("page description: ${page?.description}")
            println("page descriptionRuns: ${page?.descriptionRuns?.size}")
            println("page sections: ${page?.sections?.size}")
        }
    }
}
