package com.pause.app.di

import com.pause.app.data.repository.SettingsRepositoryImpl
import com.pause.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds interface contracts to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
