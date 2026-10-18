package com.lin0721.linmusic.core.update.data

import retrofit2.http.GET
import retrofit2.http.Path

// GitHub Releases ， API，60 /
interface GithubReleaseApi {

    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<GithubReleaseDto>
}
