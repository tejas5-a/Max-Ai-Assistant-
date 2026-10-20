package com.example.data.orchestrator

import android.content.Context
import com.example.data.remote.GeminiApiService
import com.example.data.remote.OpenRouterService
import com.example.data.remote.PollinationsService
import com.example.data.remote.WebSearchService

class ProviderRouter(
    context: Context,
    val openRouterService: OpenRouterService = OpenRouterService(),
    val pollinationsService: PollinationsService = PollinationsService(context),
    val webSearchService: WebSearchService = WebSearchService(),
    val geminiApiService: GeminiApiService = GeminiApiService()
)
