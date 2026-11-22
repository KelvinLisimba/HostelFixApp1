package com.example.hostelfixapp.data.repository

import com.example.hostelfixapp.data.model.Hostel
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.StatusHistory
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getAllReportsFlow(): Flow<List<MaintenanceReport>>
    fun getStudentReportsFlow(studentId: String): Flow<List<MaintenanceReport>>
    fun getReportById(reportId: String): MaintenanceReport?
    suspend fun submitReport(report: MaintenanceReport): Result<MaintenanceReport>
    suspend fun updateReportStatus(
        reportId: String,
        newStatus: ReportStatus,
        staffId: String,
        staffName: String,
        note: String = ""
    ): Result<MaintenanceReport>
    suspend fun assignStaff(
        reportId: String,
        staffId: String,
        staffName: String
    ): Result<MaintenanceReport>
    suspend fun confirmResolution(reportId: String): Result<MaintenanceReport>
    fun getStatusHistory(reportId: String): List<StatusHistory>
    fun getAvailableHostels(): List<Hostel>
}
