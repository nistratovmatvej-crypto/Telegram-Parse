package app.socialradar.search

/**
 * A source adapter must return only publicly discoverable entities and attach
 * provenance to every candidate. Implementations must respect source policies,
 * access controls and rate limits.
 */
interface SearchSource {
    val sourceId: String

    fun isAvailable(): Boolean

    fun discover(query: String, request: SearchRequest): List<CommunityCandidate>
}
