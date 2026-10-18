package com.lin0721.linmusic.di

import com.lin0721.linmusic.core.metadata.ITunesSearchClient
import com.lin0721.linmusic.core.metadata.MetadataCache
import com.lin0721.linmusic.core.metadata.MetadataEnricher
import com.lin0721.linmusic.core.metadata.MusicBrainzClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val metadataModule = module {
    single { ITunesSearchClient() }
    single { MusicBrainzClient() }
    single { MetadataCache(androidContext()) }
    single { MetadataEnricher(androidContext(), get(), get(), get()) }
    single { com.lin0721.linmusic.core.metadata.MimikyuArtistStatsRepository(androidContext()) }
}
