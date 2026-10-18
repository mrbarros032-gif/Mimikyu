package com.mocharealm.accompanist.lyrics.ui.internal.rendering

internal class LyricsResourceLock {
    @PublishedApi
    internal val monitor = Any()
    inline fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
