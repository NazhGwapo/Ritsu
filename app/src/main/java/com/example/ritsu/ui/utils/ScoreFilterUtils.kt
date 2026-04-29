package com.example.ritsu.ui.utils

import com.example.ritsu.data.FullScoreRecord

enum class QueryOperator {
    EQUALS, GREATER_THAN, LESS_THAN, GREATER_EQUALS, LESS_EQUALS, CONTAINS
}

data class ScoreQuery(
    val key: String?,
    val operator: QueryOperator,
    val value: String,
)

object ScoreFilterUtils {
    
    /**
     * Parses a search query string. 
     * Supports basic text search or "key op value" syntax.
     */
    fun parseQuery(query: String): List<ScoreQuery> {
        if (query.isBlank()) return emptyList()
        
        // Simple heuristic: if no operator found, treat as title/game contains search
        val operators = listOf(">=", "<=", "=", ">", "<")
        val foundOp = operators.find { query.contains(it) }
        
        if (foundOp == null) {
            return listOf(ScoreQuery(null, QueryOperator.CONTAINS, query.trim()))
        }
        
        val key = query.substringBefore(foundOp).trim().lowercase()
        val value = query.substringAfter(foundOp).trim().removeSurrounding("\"").removeSurrounding("'")
        
        val op = when (foundOp) {
            ">=" -> QueryOperator.GREATER_EQUALS
            "<=" -> QueryOperator.LESS_EQUALS
            "=" -> QueryOperator.EQUALS
            ">" -> QueryOperator.GREATER_THAN
            "<" -> QueryOperator.LESS_THAN
            else -> QueryOperator.CONTAINS
        }
        
        return listOf(ScoreQuery(key, op, value))
    }
    
    /**
     * Checks if a score record matches the parsed queries.
     */
    fun matches(record: FullScoreRecord, gameName: String, queries: List<ScoreQuery>): Boolean {
        if (queries.isEmpty()) return true
        
        val score = record.genericScore
        
        return queries.all { q ->
            if (q.key == null) {
                // Generic search in title or game name
                score.songTitle.contains(q.value, ignoreCase = true) || 
                gameName.contains(q.value, ignoreCase = true)
            } else {
                // Specific field search
                when (q.key) {
                    "title" -> compareString(score.songTitle, q.operator, q.value)
                    "game" -> compareString(gameName, q.operator, q.value)
                    "score" -> compareLong(score.totalScore, q.operator, q.value)
                    "combo" -> compareLong(score.maxCombo.toLong(), q.operator, q.value)
                    "accuracy" -> compareDouble(score.accuracy, q.operator, q.value)
                    "rank" -> compareString(score.playRank, q.operator, q.value)
                    else -> {
                        // Search in custom details
                        val detail = record.details.find { it.key.equals(q.key, ignoreCase = true) }
                        if (detail != null) {
                            val detailVal = detail.value
                            // Try numeric comparison first if possible
                            val numVal = detailVal.filter { (it.isDigit() || it == '.') }.toDoubleOrNull()
                            if (numVal != null && q.value.toDoubleOrNull() != null) {
                                compareDouble(numVal, q.operator, q.value)
                            } else {
                                compareString(detailVal, q.operator, q.value)
                            }
                        } else {
                            false
                        }
                    }
                }
            }
        }
    }
    
    private fun compareString(actual: String, op: QueryOperator, target: String): Boolean {
        return when (op) {
            QueryOperator.EQUALS -> actual.equals(target, ignoreCase = true)
            QueryOperator.CONTAINS -> actual.contains(target, ignoreCase = true)
            else -> false // String doesn't support numeric operators well here
        }
    }
    
    private fun compareLong(actual: Long, op: QueryOperator, target: String): Boolean {
        val targetVal = target.filter { it.isDigit() }.toLongOrNull() ?: return false
        return when (op) {
            QueryOperator.EQUALS -> actual == targetVal
            QueryOperator.GREATER_THAN -> actual > targetVal
            QueryOperator.LESS_THAN -> actual < targetVal
            QueryOperator.GREATER_EQUALS -> actual >= targetVal
            QueryOperator.LESS_EQUALS -> actual <= targetVal
            QueryOperator.CONTAINS -> actual.toString().contains(target)
        }
    }
    
    private fun compareDouble(actual: Double, op: QueryOperator, target: String): Boolean {
        val targetVal = target.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return false
        return when (op) {
            QueryOperator.EQUALS -> actual == targetVal
            QueryOperator.GREATER_THAN -> actual > targetVal
            QueryOperator.LESS_THAN -> actual < targetVal
            QueryOperator.GREATER_EQUALS -> actual >= targetVal
            QueryOperator.LESS_EQUALS -> actual <= targetVal
            QueryOperator.CONTAINS -> actual.toString().contains(target)
        }
    }
}
