package app.socialradar.search

import java.util.Locale

data class SearchExecutionResult(
    val candidates: List<CommunityCandidate>,
    val queriesExecuted: Int,
    val sourcesUsed: Set<String>
)

/**
 * Bounded breadth-first discovery coordinator.
 *
 * Every candidate must originate from a SearchSource implementation.
 * Call this from a background dispatcher, never from the Android main thread.
 */
class SearchCoordinator(
    private val planner: SearchQueryPlanner,
    private val sources: List<SearchSource>
) {
    fun execute(
        request: SearchRequest,
        onProgress: (SearchProgress) -> Unit = {}
    ): SearchExecutionResult {
        require(request.originalQuery.isNotBlank()) { "Search query must not be blank." }

        val maxDepth = request.maxDepth.coerceIn(0, 5)
        val maxQueries = request.maxExpansionQueries.coerceIn(1, 100)
        val queue = ArrayDeque<Pair<String, Int>>()
        val visitedQueries = linkedSetOf<String>()
        val queuedSeeds = linkedSetOf(normalize(request.originalQuery))
        val results = linkedMapOf<String, CommunityCandidate>()
        val usedSources = linkedSetOf<String>()
        var queryCount = 0

        queue.add(request.originalQuery.trim() to 0)

        while (queue.isNotEmpty() && queryCount < maxQueries) {
            val (seed, depth) = queue.removeFirst()
            val expanded = planner.expand(
                request.copy(
                    originalQuery = seed,
                    maxExpansionQueries = (maxQueries - queryCount).coerceAtLeast(1)
                )
            )

            for (query in expanded) {
                if (queryCount >= maxQueries) break
                if (!visitedQueries.add(normalize(query))) continue
                queryCount++

                val availableSources = sources.filter {
                    runCatching { it.isAvailable() }.getOrDefault(false)
                }
                onProgress(
                    SearchProgress(
                        phase = SearchPhase.DISCOVERING,
                        completedSources = usedSources.size,
                        totalSources = sources.size,
                        discoveredCount = results.size,
                        message = "Запрос $queryCount из $maxQueries: $query"
                    )
                )

                for (source in availableSources) {
                    val found = runCatching { source.discover(query, request) }
                        .getOrDefault(emptyList())
                    if (found.isNotEmpty()) usedSources += source.sourceId

                    for (candidate in found) {
                        if (!isAllowedType(candidate.type, request)) continue
                        val key = stableKey(candidate)
                        val scored = candidate.copy(
                            relevanceScore = relevance(candidate, request.originalQuery),
                            discoveredBy = candidate.discoveredBy + source.sourceId
                        )
                        val previous = results[key]
                        results[key] = if (previous == null) scored else merge(previous, scored)

                        if (depth < maxDepth) {
                            val nextSeed = candidate.title.trim()
                            val nextKey = normalize(nextSeed)
                            if (
                                nextSeed.length >= 3 &&
                                nextKey !in visitedQueries &&
                                queuedSeeds.add(nextKey) &&
                                queue.size < maxQueries * 2
                            ) {
                                queue.add(nextSeed to depth + 1)
                            }
                        }
                    }
                }
            }
        }

        val ordered = results.values.sortedWith(
            compareByDescending<CommunityCandidate> { it.relevanceScore ?: 0 }
                .thenByDescending { it.memberCount ?: -1L }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )

        onProgress(
            SearchProgress(
                phase = SearchPhase.COMPLETED,
                completedSources = usedSources.size,
                totalSources = sources.size,
                discoveredCount = ordered.size,
                message = "Поиск завершён"
            )
        )

        return SearchExecutionResult(ordered, queryCount, usedSources)
    }

    private fun isAllowedType(type: CommunityType, request: SearchRequest): Boolean =
        when (type) {
            CommunityType.GROUP -> request.includeGroups
            CommunityType.CHANNEL -> request.includeChannels
            CommunityType.DISCUSSION -> request.includeDiscussions
            CommunityType.UNKNOWN -> true
        }

    private fun stableKey(candidate: CommunityCandidate): String =
        candidate.username?.takeIf { it.isNotBlank() }
            ?.let { "u:" + normalize(it.removePrefix("@")) }
            ?: candidate.publicUrl?.takeIf { it.isNotBlank() }
                ?.let { "url:" + normalize(it.trimEnd('/')) }
            ?: "id:" + normalize(candidate.stableId)

    private fun relevance(candidate: CommunityCandidate, query: String): Int {
        val terms = query.lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 }
            .toSet()
        if (terms.isEmpty()) return 0

        val title = candidate.title.lowercase(Locale.ROOT)
        val description = candidate.description.orEmpty().lowercase(Locale.ROOT)
        val titleHits = terms.count(title::contains)
        val descriptionHits = terms.count(description::contains)
        return ((titleHits * 25 + descriptionHits * 10) / terms.size).coerceIn(0, 100)
    }

    private fun merge(a: CommunityCandidate, b: CommunityCandidate): CommunityCandidate =
        a.copy(
            title = if (a.title.length >= b.title.length) a.title else b.title,
            publicUrl = a.publicUrl ?: b.publicUrl,
            username = a.username ?: b.username,
            description = a.description?.takeIf { it.isNotBlank() } ?: b.description,
            languageCode = a.languageCode ?: b.languageCode,
            memberCount = listOfNotNull(a.memberCount, b.memberCount).maxOrNull(),
            relevanceScore = maxOf(a.relevanceScore ?: 0, b.relevanceScore ?: 0),
            discoveredBy = a.discoveredBy + b.discoveredBy,
            lastCheckedEpochSeconds = maxOf(
                a.lastCheckedEpochSeconds ?: 0L,
                b.lastCheckedEpochSeconds ?: 0L
            ).takeIf { it > 0L }
        )

    private fun normalize(value: String): String =
        value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
}
