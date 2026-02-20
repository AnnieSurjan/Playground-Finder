package com.playgroundfinder.app.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * Csak akkor ad vissza példányt, ha a google-services.json be van állítva.
     * Nélküle null – az AuthRepository és SubscriptionRepository kezeli ezt.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(@ApplicationContext context: Context): FirebaseAuth? =
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance() else null

    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): FirebaseFirestore? =
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null
}
