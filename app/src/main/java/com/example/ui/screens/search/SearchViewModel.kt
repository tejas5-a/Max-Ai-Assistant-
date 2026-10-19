package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.OpenRouterMessage
import com.example.data.model.SearchSource
import com.example.data.remote.OpenRouterService
import com.example.data.remote.WebSearchService
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val isSearching: Boolean = false,
    val isDeepResearchMode: Boolean = false,
    val query: String = "",
    val sources: List<SearchSource> = emptyList(),
    val researchSynthesis: String? = null,
    val errorMessage: String? = null
)

class SearchViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val webSearchService = WebSearchService()
    private val openRouterService = OpenRouterService()

    private val _isSearching = MutableStateFlow(false)
    private val _isDeepResearchMode = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _sources = MutableStateFlow<List<SearchSource>>(emptyList())
    private val _researchSynthesis = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SearchUiState> = combine(
        combine(_isSearching, _isDeepResearchMode, _query) { searching, deepResearch, query ->
            Triple(searching, deepResearch, query)
        },
        combine(_sources, _researchSynthesis, _errorMessage) { sources, synthesis, error ->
            Triple(sources, synthesis, error)
        }
    ) { part1, part2 ->
        SearchUiState(
            isSearching = part1.first,
            isDeepResearchMode = part1.second,
            query = part1.third,
            sources = part2.first,
            researchSynthesis = part2.second,
            errorMessage = part2.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun toggleDeepResearch() {
        _isDeepResearchMode.value = !_isDeepResearchMode.value
    }

    fun performSearch(userQuery: String) {
        if (userQuery.isBlank()) return
        _query.value = userQuery
        _isSearching.value = true
        _errorMessage.value = null
        _researchSynthesis.value = null

        viewModelScope.launch {
            val searchResult = webSearchService.performSearch(userQuery)
            _isSearching.value = false

            if (searchResult.isSuccess) {
                val results = searchResult.getOrDefault(emptyList())
                _sources.value = results

                if (results.isNotEmpty()) {
                    val settings = preferencesRepository.settingsFlow.first()
                    if (settings.isOpenRouterEnabled) {
                        val sourceText = results.joinToString("\n") { "- [${it.title}] (${it.domain}): ${it.snippet}" }
                        val messages = listOf(
                            OpenRouterMessage("system", "You are MAX AI ASSISTANT. Synthesize these live web search results into a clean, objective research summary with key findings:"),
                            OpenRouterMessage("user", "Query: $userQuery\n\nResults:\n$sourceText")
                        )
                        val aiResult = openRouterService.sendChatPrompt(
                            apiKey = settings.openRouterApiKey,
                            model = settings.selectedOpenRouterModel,
                            messages = messages
                        )
                        if (aiResult.isSuccess) {
                            _researchSynthesis.value = aiResult.getOrNull()
                        }
                    }
                } else {
                    _errorMessage.value = "No web search results found for '$userQuery'."
                }
            } else {
                _errorMessage.value = searchResult.exceptionOrNull()?.message ?: "Search failed."
            }
        }
    }
}
