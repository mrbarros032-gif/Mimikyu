package com.lin0721.linmusic.feature.search.data.dto

import kotlinx.serialization.Serializable

// /weapi/search/suggest/keyword ： result ，allMatch
@Serializable
data class SearchSuggestResponse(
    val code: Int = 0,
    val result: SearchSuggestResult? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SearchSuggestResult(
    val allMatch: List<SearchSuggestKeyword>? = null
)

@Serializable
data class SearchSuggestKeyword(
    val keyword: String = ""
)

@Serializable
data class SearchSuggestRequest(
    val s: String
)

// /weapi/search/suggest/web ：Albums picId，
@Serializable
data class SearchSuggestWebResponse(
    val code: Int = 0,
    val result: SearchSuggestWebResult? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SearchSuggestWebResult(
    val artists: List<SuggestArtist>? = null,
    val albums: List<SuggestAlbum>? = null
)

@Serializable
data class SuggestArtist(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String? = null,
    val img1v1Url: String? = null
)

@Serializable
data class SuggestAlbum(
    val id: Long = 0,
    val name: String = "",
    val artist: SuggestArtist? = null
)
