package com.example.karma.util

import com.example.karma.data.model.Rank

object RankCalculator {

    fun getRank(score: Float): Rank? {
        if (score < 0) return null
        return Rank.RANKS.find { score >= it.min && score < it.max }
    }
}
