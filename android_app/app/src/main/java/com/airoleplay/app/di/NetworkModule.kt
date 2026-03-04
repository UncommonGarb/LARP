package com.airoleplay.app.di

import com.airoleplay.app.data.remote.api.BackendAPI
import com.airoleplay.app.data.remote.api.KoboldCPPClient
import com.airoleplay.app.data.remote.api.OllamaClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().setLenient().create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // For generation streams
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Factory method to create a BackendAPI depending on the connection type
    fun createBackendAPI(type: String, baseUrl: String, client: OkHttpClient, gson: Gson): BackendAPI {
        return if (type.uppercase() == "OLLAMA") {
            OllamaClient(baseUrl, client, gson)
        } else {
            KoboldCPPClient(baseUrl, client, gson)
        }
    }
}
