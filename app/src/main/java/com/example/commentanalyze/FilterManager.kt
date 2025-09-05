package com.example.commentanalyze

object FilterManager {

    // Default all filters enabled
    private val filterStates = mutableMapOf(
        "hate" to true,
        "sexual" to true,
        "racist" to true,
        "harassment" to true
    )

    fun isEnabled(category: String): Boolean {
        return filterStates[category] ?: true
    }

    fun toggle(category: String) {
        val currentState = filterStates[category] ?: true
        filterStates[category] = !currentState
        println("FilterManager: ${category} toggled to ${!currentState}")
    }

    fun setEnabled(category: String, enabled: Boolean) {
        filterStates[category] = enabled
    }

    fun getEnabledCategories(): List<String> {
        return filterStates.filter { it.value }.keys.toList()
    }

    fun filterResults(results: List<ToxicText>): List<ToxicText> {
        return results.filter { toxicText ->
            isEnabled(toxicText.type)
        }
    }

    fun getAllCategories(): List<String> {
        return filterStates.keys.toList()
    }
}