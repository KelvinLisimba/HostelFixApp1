package com.example.hostelfixapp.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hostelfixapp.data.model.Hostel
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.StatusHistory
import com.example.hostelfixapp.data.repository.AuthRepository
import com.example.hostelfixapp.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StaffUiState(
    val allReports: List<MaintenanceReport> = emptyList(),
    val filteredReports: List<MaintenanceReport> = emptyList(),
    val availableHostels: List<Hostel> = emptyList(),
    val selectedStatusFilter: ReportStatus? = null,
    val selectedHostelFilter: String? = null,
    val isOnline: Boolean = true,
    val userMessage: String? = null
)

class StaffViewModel(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    init {
        loadStaffData()
    }

    fun loadStaffData() {
        _uiState.value = _uiState.value.copy(
            availableHostels = reportRepository.getAvailableHostels()
        )

        viewModelScope.launch {
            reportRepository.getAllReportsFlow().collect { reports ->
                _uiState.value = _uiState.value.copy(allReports = reports)
                applyFilters()
            }
        }
    }

    fun setStatusFilter(status: ReportStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        applyFilters()
    }

    fun setHostelFilter(hostelName: String?) {
        _uiState.value = _uiState.value.copy(selectedHostelFilter = hostelName)
        applyFilters()
    }

    private fun applyFilters() {
        val all = _uiState.value.allReports
        val statusFilter = _uiState.value.selectedStatusFilter
        val hostelFilter = _uiState.value.selectedHostelFilter

        val fiveMinutesInMillis = 5 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        val filtered = all.filter { report ->
            val matchesHostel = (hostelFilter == null || report.hostelName == hostelFilter)

            // FIXED reports stay for 5 minutes on staff dashboard, then go exclusively to archive records
            val isArchivedFixed = report.status == ReportStatus.FIXED &&
                    (currentTime - report.updatedAt) > fiveMinutesInMillis

            if (isArchivedFixed) {
                false // Automatically removed from active/fixed staff dashboard and placed in archive
            } else {
                val matchesStatus = (statusFilter == null || report.status == statusFilter)
                matchesStatus && matchesHostel
            }
        }
        _uiState.value = _uiState.value.copy(filteredReports = filtered)
    }

    fun assignStaff(reportId: String, staffName: String) {
        val currentStaff = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            val result = reportRepository.assignStaff(
                reportId = reportId,
                staffId = currentStaff.id,
                staffName = staffName
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(userMessage = "Report assigned to $staffName")
            } else {
                _uiState.value = _uiState.value.copy(userMessage = "Failed to assign staff.")
            }
        }
    }

    fun updateStatus(reportId: String, newStatus: ReportStatus, note: String) {
        val currentStaff = authRepository.getCurrentUser() ?: return
        val report = reportRepository.getReportById(reportId)

        if ((newStatus == ReportStatus.IN_PROGRESS || newStatus == ReportStatus.FIXED) && report?.assignedStaffName.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                userMessage = "⚠️ Please assign a staff member before marking as ${newStatus.displayName}."
            )
            return
        }

        viewModelScope.launch {
            val result = reportRepository.updateReportStatus(
                reportId = reportId,
                newStatus = newStatus,
                staffId = currentStaff.id,
                staffName = currentStaff.name,
                note = note
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    userMessage = "Status updated to ${newStatus.displayName}"
                )
            } else {
                _uiState.value = _uiState.value.copy(userMessage = "Failed to update status.")
            }
        }
    }

    fun getReportById(reportId: String): MaintenanceReport? {
        return reportRepository.getReportById(reportId)
    }

    fun unarchiveReport(reportId: String) {
        val currentStaff = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            val result = reportRepository.updateReportStatus(
                reportId = reportId,
                newStatus = ReportStatus.IN_PROGRESS,
                staffId = currentStaff.id,
                staffName = currentStaff.name,
                note = "Moved report back to active maintenance dashboard"
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(userMessage = "Report moved back to Active Dashboard!")
                loadStaffData()
            }
        }
    }

    fun getStatusHistory(reportId: String): List<StatusHistory> {
        return reportRepository.getStatusHistory(reportId)
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
