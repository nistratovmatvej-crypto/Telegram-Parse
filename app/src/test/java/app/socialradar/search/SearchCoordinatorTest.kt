package app.socialradar.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCoordinatorTest {

    @Test
    fun mergesDuplicatePublicUsernamesFromMultipleSources() {
        val first = FakeSource(
            "catalog-a",
            listOf(candidate(description = "Инди-разработка игр", members = 120))
        )
        val second = FakeSource(
            "catalog-b",
            listOf(candidate(description = "Сообщество разработчиков", members = 240))
        )
        val coordinator = SearchCoordinator(SearchQueryPlanner(), listOf(first, second))

        val result = coordinator.execute(
            SearchRequest(
                originalQuery = "разработка игр",
                maxExpansionQueries = 1,
                maxDepth = 0
            )
        )

        assertEquals(1, result.candidates.size)
        assertEquals(240L, result.candidates.single().memberCount)
        assertEquals(setOf("catalog-a", "catalog-b"), result.candidates.single().discoveredBy)
    }

    @Test
    fun excludesDisabledCommunityTypes() {
        val source = FakeSource(
            "fixture",
            listOf(candidate(type = CommunityType.CHANNEL))
        )
        val result = SearchCoordinator(SearchQueryPlanner(), listOf(source)).execute(
            SearchRequest(
                originalQuery = "game dev",
                includeChannels = false,
                maxExpansionQueries = 1,
                maxDepth = 0
            )
        )

        assertTrue(result.candidates.isEmpty())
    }

    private fun candidate(
        type: CommunityType = CommunityType.GROUP,
        description: String? = null,
        members: Long? = null
    ) = CommunityCandidate(
        stableId = "fixture-1",
        title = "Game Dev RU",
        publicUrl = "https://t.me/gamedev_ru",
        username = "gamedev_ru",
        type = type,
        description = description,
        languageCode = "ru",
        memberCount = members,
        relevanceScore = null,
        verificationStatus = VerificationStatus.UNVERIFIED,
        discoveredBy = emptySet(),
        lastCheckedEpochSeconds = null
    )

    private class FakeSource(
        override val sourceId: String,
        private val data: List<CommunityCandidate>
    ) : SearchSource {
        override fun isAvailable() = true
        override fun discover(query: String, request: SearchRequest) = data
    }
}
