package io.hansu.pacer.domain.stat

import java.time.LocalDate

data class DailyStatsId(
    var userId: Long = 0,
    var statDate: LocalDate = LocalDate.MIN
) : java.io.Serializable