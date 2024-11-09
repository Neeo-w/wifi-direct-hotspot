package com.example.wifip2photspot

import java.time.LocalDate

data class DataUsageRecord(
    val date: LocalDate,
    val rxBytes: Long,
    val txBytes: Long
)