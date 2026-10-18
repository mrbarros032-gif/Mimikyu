package com.lin0721.linmusic.feature.search.data

import com.lin0721.linmusic.feature.search.domain.HotSearch
import com.lin0721.linmusic.feature.search.domain.PlaylistCategoryPage
import com.lin0721.linmusic.feature.search.domain.PlaylistTag
import com.lin0721.linmusic.feature.search.domain.SearchPageResult
import com.lin0721.linmusic.feature.search.domain.SearchSuggestion
import com.lin0721.linmusic.feature.search.domain.SearchType
import kotlinx.coroutines.flow.Flow

// Search（search ）
interface SearchRepository {

    // Search
    fun getDefaultSearchKeyword(): Flow<Result<String>>

    // Search
    fun search(keyword: String, type: SearchType, offset: Int = 0, limit: Int = 30): Flow<Result<SearchPageResult>>

    // Trending
    fun getHotSearches(): Flow<Result<List<HotSearch>>>

    // Curated Playlists（）
    fun getPlaylistTags(): Flow<Result<List<PlaylistTag>>>


    fun getSuggestions(keyword: String): Flow<Result<List<SearchSuggestion>>>

    // Curated Playlists，cursor  of  lasttime
    fun getPlaylistsByCategory(category: String, cursor: Long = 0, limit: Int = 30): Flow<Result<PlaylistCategoryPage>>
}
