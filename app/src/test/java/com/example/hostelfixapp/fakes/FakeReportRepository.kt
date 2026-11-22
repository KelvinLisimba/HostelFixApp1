package com.example.hostelfixapp.fakes

import com.example.hostelfixapp.data.model.Hostel
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.StatusHistory
import com.example.hostelfixapp.data.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeReportRepository : ReportRepository {

    private val reportsList = mutableListOf<MaintenanceReport>()
    private val reportsFlow = MutableStateFlow<List<MaintenanceReport>>(emptyList())
    private val historyMap = mutableMapOf<String, MutableList<StatusHistory>>()

    private val hostels = listOf(
        Hostel("h1", "Kenyatta", (1..25).map { "Room $it" }),
        Hostel("h2", "Kachere", (1..25).map { "Room $it" })
    )

    fun setReports(reports: List<MaintenanceReport>) {
        reportsList.clear()
        reportsList.addAll(reports)
        reportsFlow.value = reportsList.toList()
    }

    override fun getAllReportsFlow(): Flow<List<MaintenanceReport>> {
        return reportsFlow.asStateFlow()
    }

    override fun getStudentReportsFlow(studentId: String): Flow<List<MaintenanceReport>> {
        return reportsFlow.asStateFlow().map { reports ->
            reports.filter { it.studentId == studentId }
        }
    }

    override fun getReportById(reportId: String): MaintenanceReport? {
        return reportsList.find { it.reportId == reportId }
    }

    override suspend fun submitReport(report: MaintenanceReport): Result<MaintenanceReport> {
        val finalReportId = if (report.reportId.isBlank()) "REP_${System.currentTimeMillis()}" else report.reportId
        val newReport = report.copy(
            reportId = finalReportId,
            status = ReportStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        reportsList.add(newReport)
        reportsFlow.value = reportsList.toList()
        return Result.success(newReport)
    }

    override suspend fun updateReportStatus(
        reportId: String,
        newStatus: ReportStatus,
        staffId: String,
        staffName: String,
        note: String
    ): Result<MaintenanceReport> {
        val index = reportsList.indexOfFirst { it.reportId == reportId }
        if (index == -1) return Result.failure(Exception("Report not found"))

        val existing = reportsList[index]
        val updated = existing.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        reportsList[index] = updated
        reportsFlow.value = reportsList.toList()

        val history = StatusHistory(
            historyId = "HIST_${System.currentTimeMillis()}",
            reportId = reportId,
            previousStatus = existing.status,
            newStatus = newStatus,
            updatedByStaffId = staffId,
            updatedByStaffName = staffName,
            note = note,
            timestamp = System.currentTimeMillis()
        )
        historyMap.getOrPut(reportId) { mutableListOf() }.add(history)

        return Result.success(updated)
    }

    override suspend fun assignStaff(
        reportId: String,
        staffId: String,
        staffName: String
    ): Result<MaintenanceReport> {
        val index = reportsList.indexOfFirst { it.reportId == reportId }
        if (index == -1) return Result.failure(Exception("Report not found"))

        val existing = reportsList[index]
        val updated = existing.copy(
            assignedStaffId = staffId,
            assignedStaffName = staffName,
            updatedAt = System.currentTimeMillis()
        )
        reportsList[index] = updated
        reportsFlow.value = reportsList.toList()
        return Result.success(updated)
    }

    override suspend fun confirmResolution(reportId: String): Result<MaintenanceReport> {
        val index = reportsList.indexOfFirst { it.reportId == reportId }
        if (index == -1) return Result.failure(Exception("Report not found"))

        val existing = reportsList[index]
        val updated = existing.copy(
            isConfirmedByStudent = true,
            updatedAt = System.currentTimeMillis()
        )
        reportsList[index] = updated
        reportsFlow.value = reportsList.toList()
        return Result.success(updated)
    }

    override fun getStatusHistory(reportId: String): List<StatusHistory> {
        return historyMap[reportId] ?: emptyList()
    }

    override fun getAvailableHostels(): List<Hostel> {
        return hostels
    }
}
