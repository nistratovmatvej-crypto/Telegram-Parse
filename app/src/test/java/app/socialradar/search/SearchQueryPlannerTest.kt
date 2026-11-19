package app.socialradar.search

import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryPlannerTest {
    private val planner = SearchQueryPlanner()

    @Test
    fun expandsRussianGameDevelopmentQuery() {
        val result = planner.expand(
            SearchRequest(
                originalQuery = "разработка игр с искусственным интеллектом",
                languageCodes = setOf("ru", "en")
            )
        )

        assertTrue(result.contains("разработка игр с искусственным интеллектом"))
        assertTrue(result.any { it.contains("gamedev", ignoreCase = true) })
        assertTrue(result.any { it.contains("Telegram", ignoreCase = true) })
    }

    @Test
    fun emptyQueryReturnsNoSearches() {
        assertTrue(planner.expand(SearchRequest(originalQuery = "   ")).isEmpty())
    }

    @Test
    fun expansionRespectsConfiguredLimit() {
        val result = planner.expand(
            SearchRequest(
                originalQuery = "разработка игр",
                maxExpansionQueries = 3
            )
        )

        assertTrue(result.size <= 3)
    }
}
