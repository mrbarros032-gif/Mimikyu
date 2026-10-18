package com.lin0721.linmusic.feature.playlist.domain

import com.lin0721.linmusic.feature.playlist.data.PlaylistRepository

class UpdatePlaylistCoverUseCase(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, imageBytes: ByteArray): Result<String> {
        return Result.failure(UnsupportedOperationException("Cloud cover upload is not supported"))
    }
}
