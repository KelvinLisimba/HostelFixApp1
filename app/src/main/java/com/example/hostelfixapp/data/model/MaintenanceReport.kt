package com.example.hostelfixapp.data.model

enum class ReportCategory(val displayName: String) {
    ELECTRICAL("Electrical"),
    PLUMBING("Plumbing"),
    FURNITURE("Furniture"),
    DOORS_WINDOWS("Doors & Windows"),
    OTHER("Other")
}

enum class ReportStatus(val displayName: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    FIXED("Fixed")
}

data class MaintenanceReport(
    val reportId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val hostelName: String = "",
    val roomNumber: String = "",
    val category: ReportCategory = ReportCategory.OTHER,
    val description: String = "",
    val photoUrl: String? = null,
    val status: ReportStatus = ReportStatus.PENDING,
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isConfirmedByStudent: Boolean = false
)
