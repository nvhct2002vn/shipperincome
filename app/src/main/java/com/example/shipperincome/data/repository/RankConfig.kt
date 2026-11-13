package com.example.shipperincome.data.repository

import java.time.LocalDate

/**
 * Cau hinh xep hang: can [targetPoints] diem trong moi chu ky [cycleDays] ngay.
 * [cycleAnchor] la ngay bat dau cua mot chu ky bat ky; cac chu ky truoc/sau tu suy ra tu ngay nay.
 */
data class RankConfig(
    val targetPoints: Int,
    val cycleDays: Int,
    val cycleAnchor: LocalDate?
)
