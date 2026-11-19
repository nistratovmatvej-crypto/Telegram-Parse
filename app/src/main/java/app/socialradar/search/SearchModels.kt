package app.socialradar.search

enum class CommunityType {
    GROUP,
    CHANNEL,
    DISCUSSION,
    UNKNOWN
}

enum class VerificationStatus {
    UNVERIFIED,
    REACHABLE,
    UNAVAILABLE,
    RESTRICTED,
    UNKNOWN
}

data class CommunityCandidate(
    val stableId: String,
    val title: String,
    val publicUrl: String?,
    val username: String?,
    val type: CommunityType,
    val description: String?,
    val languageCode: String?,
    val memberCount: Long?,
    val relevanceScore: Int?,
    val verificationStatus: VerificationStatus,
    val discoveredBy: Set<String>,
    val lastCheckedEpochSeconds: Long?
)

data class SearchRequest(
    val originalQuery: String,
    val languageCodes: Set<String> = setOf("ru", "en"),
    val includeGroups: Boolean = true,
    val includeChannels: Boolean = true,
    val includeDiscussions: Boolean = true,
    val maxExpansionQueries: Int = 30,
    val maxDepth: Int = 3
)

data class SearchProgress(
    val phase: SearchPhase,
    val completedSources: Int,
    val totalSources: Int,
    val discoveredCount: Int,
    val message: String
)

enum class SearchPhase {
    PREPARING,
    DISCOVERING,
    NORMALIZING,
    DEDUPLICATING,
    RANKING,
    VERIFYING,
    COMPLETED,
    PAUSED,
    FAILED
}
