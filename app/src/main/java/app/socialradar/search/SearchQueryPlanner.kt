package app.socialradar.search

import java.util.Locale

/**
 * Deterministic first-pass query expansion.
 *
 * This is deliberately not presented as semantic AI. Later milestones can add an
 * on-device model while keeping this planner as a fast offline fallback.
 */
class SearchQueryPlanner {

    private val phraseExpansions = mapOf(
        "разработка игр" to listOf("gamedev", "game development", "создание видеоигр"),
        "искусственный интеллект" to listOf("AI", "нейросети", "artificial intelligence"),
        "нейросети" to listOf("AI", "machine learning", "генеративный ИИ"),
        "программирование" to listOf("programming", "разработка ПО", "software development"),
        "дизайн" to listOf("design", "UX UI", "графический дизайн"),
        "криптовалюта" to listOf("crypto", "blockchain", "web3")
    )

    fun expand(request: SearchRequest): List<String> {
        val original = request.originalQuery.trim()
        if (original.isEmpty()) return emptyList()

        val candidates = linkedSetOf(original)
        val normalized = original.lowercase(Locale.ROOT)

        phraseExpansions.forEach { (phrase, expansions) ->
            if (normalized.contains(phrase)) {
                expansions.forEach { expansion ->
                    candidates += replaceIgnoreCase(original, phrase, expansion)
                }
            }
        }

        // Add Telegram-specific discovery forms without assuming a particular source.
        candidates += "$original Telegram"
        candidates += "$original чат"
        candidates += "$original группа"
        candidates += "$original канал"

        if ("ru" in request.languageCodes) {
            candidates += "$original сообщество"
        }
        if ("en" in request.languageCodes) {
            candidates += "$original community"
            candidates += "$original group"
        }

        return candidates
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(request.maxExpansionQueries.coerceIn(1, 100))
            .toList()
    }

    private fun replaceIgnoreCase(source: String, target: String, replacement: String): String {
        val index = source.indexOf(target, ignoreCase = true)
        if (index < 0) return source
        return source.replaceRange(index, index + target.length, replacement)
    }
}
