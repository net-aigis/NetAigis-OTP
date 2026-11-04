package de.wittmer.app.otp.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.wittmer.app.otp.feature.FeatureAccess
import de.wittmer.app.otp.feature.FeatureAccessImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureModule {
    @Binds
    @Singleton
    abstract fun bindFeatureAccess(impl: FeatureAccessImpl): FeatureAccess
}
