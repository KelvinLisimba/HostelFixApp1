package com.example.hostelfixapp.ui.student

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hostelfixapp.data.model.Hostel
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.StatusHistory
import com.example.hostelfixapp.data.repository.AuthRepository
import com.example.hostelfixapp.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudentUiState(
    val studentReports: List<MaintenanceReport> = emptyList(),
    val archivedReports: List<MaintenanceReport> = emptyList(),
    val availableHostels: List<Hostel> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOnline: Boolean = true,
    val userMessage: String? = null
)

class StudentViewModel(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    init {
        loadStudentData()
    }

    fun loadStudentData() {
        val currentUser = authRepository.getCurrentUser() ?: return
        _uiState.value = _uiState.value.copy(
            availableHostels = reportRepository.getAvailableHostels()
        )

        viewModelScope.launch {
            reportRepository.getStudentReportsFlow(currentUser.id).collect { allReports ->
                val fiveMinutesInMillis = 5 * 60 * 1000L
                val currentTime = System.currentTimeMillis()

                // FIXED reports stay for 5 minutes on active student portal, then go to archive
                val active = allReports.filter { report ->
                    if (report.status == ReportStatus.FIXED) {
                        (currentTime - report.updatedAt) <= fiveMinutesInMillis
                    } else {
                        true
                    }
                }

                val archived = allReports.filter { report ->
                    report.status == ReportStatus.FIXED && (currentTime - report.updatedAt) > fiveMinutesInMillis
                }

                _uiState.value = _uiState.value.copy(
                    studentReports = active,
                    archivedReports = archived
                )
            }
        }
    }

    fun submitReport(
        hostelName: String,
        roomNumber: String,
        category: ReportCategory,
        description: String,
        photoUri: Uri?,
        onSuccess: () -> Unit
    ) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(userMessage = "User session expired. Please sign in again.")
            return
        }

        if (hostelName.isBlank() || roomNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(userMessage = "Please select your Hostel and Room number.")
            return
        }

        if (description.isBlank()) {
            _uiState.value = _uiState.value.copy(userMessage = "Please provide a description of the problem.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, userMessage = null)

            val report = MaintenanceReport(
                studentId = currentUser.id,
                studentName = currentUser.name,
                hostelName = hostelName,
                roomNumber = roomNumber,
                category = category,
                description = description.trim(),
                photoUrl = photoUri?.toString()
            )

            val result = reportRepository.submitReport(report)
            _uiState.value = _uiState.value.copy(isSubmitting = false)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(userMessage = "Maintenance report submitted successfully!")
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    userMessage = result.exceptionOrNull()?.message ?: "Failed to submit report."
                )
            }
        }
    }

    fun confirmResolution(reportId: String) {
        viewModelScope.launch {
            val result = reportRepository.confirmResolution(reportId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(userMessage = "Thank you! Problem resolution confirmed.")
            } else {
                _uiState.value = _uiState.value.copy(userMessage = "Could not update status.")
            }
        }
    }

    fun unarchiveReport(reportId: String) {
        viewModelScope.launch {
            val report = reportRepository.getReportById(reportId) ?: return@launch
            val updated = report.copy(updatedAt = System.currentTimeMillis())
            val result = reportRepository.submitReport(updated)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(userMessage = "Report moved back to Active Dashboard!")
                loadStudentData()
            }
        }
    }

    fun getReportById(reportId: String): MaintenanceReport? {
        return reportRepository.getReportById(reportId)
    }

    fun getStatusHistory(reportId: String): List<StatusHistory> {
        return reportRepository.getStatusHistory(reportId)
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
