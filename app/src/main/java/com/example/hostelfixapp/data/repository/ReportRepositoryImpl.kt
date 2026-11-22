package com.example.hostelfixapp.data.repository

import android.content.Context
import com.example.hostelfixapp.data.local.HostelFixLocalDb
import com.example.hostelfixapp.data.model.Hostel
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.StatusHistory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ReportRepositoryImpl(context: Context) : ReportRepository {

    private val localDb = HostelFixLocalDb(context)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val historyMap = mutableMapOf<String, MutableList<StatusHistory>>()

    private val _allReportsFlow = MutableStateFlow<List<MaintenanceReport>>(emptyList())

    private val hostels = listOf(
        Hostel("h1", "Kenyatta", (1..25).map { "Room $it" }),
        Hostel("h2", "Kachere", (1..25).map { "Room $it" }),
        Hostel("h3", "Mumba", (1..25).map { "Room $it" }),
        Hostel("h4", "Chingwe", (1..25).map { "Room $it" }),
        Hostel("h5", "Umodzi", (1..25).map { "Room $it" }),
        Hostel("h6", "Kamuzu", (1..25).map { "Room $it" }),
        Hostel("h7", "Bettie Trust", (1..25).map { "Room $it" }),
        Hostel("h8", "Sangala", (1..25).map { "Room $it" }),
        Hostel("h9", "Kanjedza", (1..25).map { "Room $it" }),
        Hostel("h10", "Khondowe", (1..25).map { "Room $it" }),
        Hostel("h11", "Chilunga", (1..25).map { "Room $it" })
    )

    init {
        // 1. Load offline cached reports from local DB
        loadFromDb()

        // 2. Attach Firebase Firestore Real-Time Snapshot Listener for Cross-Device Syncing
        listenForRealtimeReports()
    }

    private fun listenForRealtimeReports() {
        try {
            firestore.collection("reports").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    try {
                        val reportId = doc.getString("reportId") ?: doc.id
                        val report = MaintenanceReport(
                            reportId = reportId,
                            studentId = doc.getString("studentId") ?: "",
                            studentName = doc.getString("studentName") ?: "Student",
                            hostelName = doc.getString("hostelName") ?: "",
                            roomNumber = doc.getString("roomNumber") ?: "",
                            category = try {
                                ReportCategory.valueOf(doc.getString("category") ?: ReportCategory.OTHER.name)
                            } catch (_: Exception) { ReportCategory.OTHER },
                            description = doc.getString("description") ?: "",
                            photoUrl = doc.getString("photoUrl"),
                            status = try {
                                ReportStatus.valueOf(doc.getString("status") ?: ReportStatus.PENDING.name)
                            } catch (_: Exception) { ReportStatus.PENDING },
                            assignedStaffId = doc.getString("assignedStaffId"),
                            assignedStaffName = doc.getString("assignedStaffName"),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                            isConfirmedByStudent = doc.getBoolean("isConfirmedByStudent") ?: false
                        )

                        // Save into local SQLite DB for offline access
                        localDb.saveReport(report)
                    } catch (_: Exception) {}
                }

                // Update UI state in real-time across all connected devices!
                loadFromDb()
            }
        } catch (_: Exception) {}
    }

    private fun saveReportToFirestore(report: MaintenanceReport) {
        try {
            val reportMap = mapOf(
                "reportId" to report.reportId,
                "studentId" to report.studentId,
                "studentName" to report.studentName,
                "hostelName" to report.hostelName,
                "roomNumber" to report.roomNumber,
                "category" to report.category.name,
                "description" to report.description,
                "photoUrl" to (report.photoUrl ?: ""),
                "status" to report.status.name,
                "assignedStaffId" to (report.assignedStaffId ?: ""),
                "assignedStaffName" to (report.assignedStaffName ?: ""),
                "createdAt" to report.createdAt,
                "updatedAt" to report.updatedAt,
                "isConfirmedByStudent" to report.isConfirmedByStudent
            )
            firestore.collection("reports").document(report.reportId).set(reportMap)
        } catch (_: Exception) {}
    }

    private fun loadFromDb() {
        val dbReports = localDb.getAllReports()
        _allReportsFlow.value = dbReports.sortedByDescending { it.createdAt }
    }

    override fun getAllReportsFlow(): Flow<List<MaintenanceReport>> {
        return _allReportsFlow.asStateFlow()
    }

    override fun getStudentReportsFlow(studentId: String): Flow<List<MaintenanceReport>> {
        return _allReportsFlow.asStateFlow().map { reports ->
            reports.filter { it.studentId == studentId }
        }
    }

    override fun getReportById(reportId: String): MaintenanceReport? {
        return _allReportsFlow.value.find { it.reportId == reportId }
    }

    override suspend fun submitReport(report: MaintenanceReport): Result<MaintenanceReport> {
        val finalReportId = if (report.reportId.isBlank()) "REP_${System.currentTimeMillis()}" else report.reportId
        val newReport = report.copy(
            reportId = finalReportId,
            status = ReportStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 1. Save locally for instant UI update & offline backup
        localDb.saveReport(newReport)

        // 2. Publish to Firebase Cloud Firestore for real-time delivery to Staff/Admin
        saveReportToFirestore(newReport)

        val initialHistory = StatusHistory(
            historyId = "HIST_${System.currentTimeMillis()}",
            reportId = finalReportId,
            previousStatus = ReportStatus.PENDING,
            newStatus = ReportStatus.PENDING,
            updatedByStaffId = "system",
            updatedByStaffName = "System",
            note = "Submitted by student ${report.studentName}",
            timestamp = System.currentTimeMillis()
        )
        historyMap.getOrPut(finalReportId) { mutableListOf() }.add(initialHistory)

        loadFromDb()
        return Result.success(newReport)
    }

    override suspend fun updateReportStatus(
        reportId: String,
        newStatus: ReportStatus,
        staffId: String,
        staffName: String,
        note: String
    ): Result<MaintenanceReport> {
        val existing = getReportById(reportId) ?: return Result.failure(Exception("Report not found"))
        val previousStatus = existing.status

        val updated = existing.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        localDb.saveReport(updated)
        saveReportToFirestore(updated)

        val history = StatusHistory(
            historyId = "HIST_${System.currentTimeMillis()}",
            reportId = reportId,
            previousStatus = previousStatus,
            newStatus = newStatus,
            updatedByStaffId = staffId,
            updatedByStaffName = staffName,
            note = note.ifBlank { "Status updated to ${newStatus.displayName}" },
            timestamp = System.currentTimeMillis()
        )
        historyMap.getOrPut(reportId) { mutableListOf() }.add(history)

        loadFromDb()
        return Result.success(updated)
    }

    override suspend fun assignStaff(
        reportId: String,
        staffId: String,
        staffName: String
    ): Result<MaintenanceReport> {
        val existing = getReportById(reportId) ?: return Result.failure(Exception("Report not found"))
        val updated = existing.copy(
            assignedStaffId = staffId,
            assignedStaffName = staffName,
            updatedAt = System.currentTimeMillis()
        )
        localDb.saveReport(updated)
        saveReportToFirestore(updated)

        loadFromDb()
        return Result.success(updated)
    }

    override suspend fun confirmResolution(reportId: String): Result<MaintenanceReport> {
        val existing = getReportById(reportId) ?: return Result.failure(Exception("Report not found"))
        val updated = existing.copy(
            isConfirmedByStudent = true,
            updatedAt = System.currentTimeMillis()
        )
        localDb.saveReport(updated)
        saveReportToFirestore(updated)

        loadFromDb()
        return Result.success(updated)
    }

    override fun getStatusHistory(reportId: String): List<StatusHistory> {
        return historyMap[reportId]?.toList() ?: emptyList()
    }

    override fun getAvailableHostels(): List<Hostel> {
        return hostels
    }
}
