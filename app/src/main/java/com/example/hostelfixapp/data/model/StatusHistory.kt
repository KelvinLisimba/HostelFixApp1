package com.example.hostelfixapp.data.model

data class StatusHistory(
    val historyId: String = "",
    val reportId: String = "",
    val previousStatus: ReportStatus = ReportStatus.PENDING,
    val newStatus: ReportStatus = ReportStatus.PENDING,
    val updatedByStaffId: String = "",
    val updatedByStaffName: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
