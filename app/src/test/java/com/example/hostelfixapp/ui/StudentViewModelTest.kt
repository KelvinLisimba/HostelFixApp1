package com.example.hostelfixapp.ui

import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.fakes.FakeAuthRepository
import com.example.hostelfixapp.fakes.FakeReportRepository
import com.example.hostelfixapp.ui.student.StudentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudentViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeReportRepository: FakeReportRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: StudentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeReportRepository = FakeReportRepository()
        fakeAuthRepository = FakeAuthRepository(
            User("student1", "Alex Student", "alex@example.com", "ST123", UserRole.STUDENT, "Kenyatta", "Room 101")
        )
        viewModel = StudentViewModel(fakeReportRepository, fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submitReport_withMissingHostel_setsErrorMessage() {
        viewModel.submitReport(
            hostelName = "",
            roomNumber = "Room 101",
            category = ReportCategory.PLUMBING,
            description = "Water leaking from sink",
            photoUri = null,
            onSuccess = {}
        )

        val uiState = viewModel.uiState.value
        assertEquals("Please select your Hostel and Room number.", uiState.userMessage)
    }

    @Test
    fun submitReport_withMissingDescription_setsErrorMessage() {
        viewModel.submitReport(
            hostelName = "Kenyatta",
            roomNumber = "Room 101",
            category = ReportCategory.PLUMBING,
            description = "",
            photoUri = null,
            onSuccess = {}
        )

        val uiState = viewModel.uiState.value
        assertEquals("Please provide a description of the problem.", uiState.userMessage)
    }

    @Test
    fun submitReport_withValidData_submitsSuccessfully() {
        var successCalled = false

        viewModel.submitReport(
            hostelName = "Kenyatta",
            roomNumber = "Room 101",
            category = ReportCategory.PLUMBING,
            description = "Water leaking from sink",
            photoUri = null,
            onSuccess = { successCalled = true }
        )

        val uiState = viewModel.uiState.value
        assertTrue(successCalled)
        assertEquals("Maintenance report submitted successfully!", uiState.userMessage)
        assertEquals(1, uiState.studentReports.size)
        assertEquals("Water leaking from sink", uiState.studentReports[0].description)
    }

    @Test
    fun confirmResolution_updatesConfirmationStatus() {
        val report = MaintenanceReport(
            reportId = "REP_TEST_CONFIRM",
            studentId = "student1",
            studentName = "Alex Student",
            hostelName = "Kenyatta",
            roomNumber = "Room 101",
            category = ReportCategory.PLUMBING,
            description = "Tap broken",
            status = ReportStatus.FIXED
        )
        fakeReportRepository.setReports(listOf(report))
        viewModel.loadStudentData()

        viewModel.confirmResolution("REP_TEST_CONFIRM")

        val uiState = viewModel.uiState.value
        assertEquals("Thank you! Problem resolution confirmed.", uiState.userMessage)
        val updatedReport = viewModel.getReportById("REP_TEST_CONFIRM")
        assertTrue(updatedReport?.isConfirmedByStudent == true)
    }

    @Test
    fun getReportById_returnsCorrectReport() {
        val report = MaintenanceReport(
            reportId = "REP_LOOKUP",
            studentId = "student1",
            studentName = "Alex Student",
            hostelName = "Kenyatta",
            roomNumber = "Room 05",
            category = ReportCategory.ELECTRICAL,
            description = "Switch issue"
        )
        fakeReportRepository.setReports(listOf(report))

        val retrieved = viewModel.getReportById("REP_LOOKUP")
        assertNotNull(retrieved)
        assertEquals("Switch issue", retrieved?.description)
    }

    @Test
    fun getAvailableHostels_returnsHostelList() {
        val hostels = viewModel.uiState.value.availableHostels
        assertTrue(hostels.isNotEmpty())
        assertTrue(hostels.any { it.name == "Kenyatta" })
    }

    @Test
    fun clearUserMessage_resetsMessageToNull() {
        viewModel.submitReport("", "", ReportCategory.ELECTRICAL, "", null) {}
        assertNotNull(viewModel.uiState.value.userMessage)

        viewModel.clearUserMessage()
        assertNull(viewModel.uiState.value.userMessage)
    }
}
