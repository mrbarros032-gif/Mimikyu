package com.lin0721.linmusic.core.update.data

import com.lin0721.linmusic.BuildConfig
import com.lin0721.linmusic.core.update.domain.UpdateInfo
import com.lin0721.linmusic.core.update.domain.VersionCodeResolver

class UpdateRepository(private val api: GithubReleaseApi) {

    // versionCode  of ，CurrentInstall"New update available"
    suspend fun fetchLatestUpdate(allowPrerelease: Boolean): Result<UpdateInfo?> {
        return try {
            val releases = api.listReleases(GITHUB_OWNER, GITHUB_REPO)
            val candidate = releases
                .asSequence()
                .filterNot { it.draft }
                .filter { allowPrerelease || !it.prerelease }
                .mapNotNull { dto ->
                    val versionCode = VersionCodeResolver.resolve(dto.tagName) ?: return@mapNotNull null
                    val apkUrl = dto.assets.firstOrNull { it.name.endsWith(".apk") }?.browserDownloadUrl
                        ?: return@mapNotNull null
                    UpdateInfo(
                        versionName = dto.tagName,
                        versionCode = versionCode,
                        isPrerelease = dto.prerelease,
                        changelog = dto.body.orEmpty(),
                        apkDownloadUrl = apkUrl,
                        releasePageUrl = dto.htmlUrl
                    )
                }
                .maxByOrNull { it.versionCode }

            val hasUpdate = candidate != null && candidate.versionCode > BuildConfig.VERSION_CODE
            Result.success(if (hasUpdate) candidate else null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val GITHUB_OWNER = "szzsuke"
        private const val GITHUB_REPO = "Mimikyu"
    }
}
