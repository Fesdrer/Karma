package com.example.karma.util

import com.example.karma.data.model.Rank

object RankCalculator {

    fun getRank(score: Float, ranks: List<Rank>): Rank? {
        if (score < 0) return null
        return ranks.find { score >= it.min && score < it.max }
    }
}
