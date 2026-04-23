package com.restaurandes.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.data.repository.LocationRepositoryImpl
import com.restaurandes.data.repository.RestaurantAnalyticsRepositoryImpl
import com.restaurandes.data.repository.ReviewRepositoryImpl
import com.restaurandes.data.repository.RestaurantRepositoryImpl
import com.restaurandes.data.repository.UserRepositoryImpl
import com.restaurandes.domain.repository.LocationRepository
import com.restaurandes.domain.repository.RestaurantAnalyticsRepository
import com.restaurandes.domain.repository.ReviewRepository
import com.restaurandes.domain.repository.RestaurantRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAnalyticsService(): AnalyticsService {
        return AnalyticsService()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideRestaurantRepository(
        firestore: FirebaseFirestore
    ): RestaurantRepository {
        return RestaurantRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideReviewRepository(
        firestore: FirebaseFirestore
    ): ReviewRepository {
        return ReviewRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(analyticsService: AnalyticsService): UserRepository {
        return UserRepositoryImpl(analyticsService)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(@ApplicationContext context: Context): LocationRepository {
        return LocationRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideRestaurantAnalyticsRepository(
        firestore: FirebaseFirestore
    ): RestaurantAnalyticsRepository {
        return RestaurantAnalyticsRepositoryImpl(firestore)
    }
}
