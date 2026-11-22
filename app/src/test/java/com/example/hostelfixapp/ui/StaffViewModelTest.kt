package com.example.hostelfixapp.ui

import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.fakes.FakeAuthRepository
import com.example.hostelfixapp.fakes.FakeReportRepository
import com.example.hostelfixapp.ui.staff.StaffViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeReportRepository: FakeReportRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: StaffViewModel

    private val report1 = MaintenanceReport(
        reportId = "REP_001",
        studentId = "ST_01",
        studentName = "Alex",
        hostelName = "Kenyatta",
        roomNumber = "Room 10",
        category = ReportCategory.ELECTRICAL,
        description = "Light bulb flickering",
        status = ReportStatus.PENDING,
        assignedStaffName = null
    )

    private val report2 = MaintenanceReport(
        reportId = "REP_002",
        studentId = "ST_02",
        studentName = "Bob",
        hostelName = "Mumba",
        roomNumber = "Room 04",
        category = ReportCategory.PLUMBING,
        description = "Leakage",
        status = ReportStatus.IN_PROGRESS,
        assignedStaffName = "Sarah Plumber"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeReportRepository = FakeReportRepository()
        fakeReportRepository.setReports(listOf(report1, report2))

        fakeAuthRepository = FakeAuthRepository(
            User("staff1", "John Maintenance", "staff@example.com", "STAFF01", UserRole.STAFF, "Kenyatta", "Office")
        )
        viewModel = StaffViewModel(fakeReportRepository, fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateStatus_toInProgress_withoutAssignedStaff_blocksAndShowsWarning() {
        viewModel.updateStatus("REP_001", ReportStatus.IN_PROGRESS, "Starting work")

        val uiState = viewModel.uiState.value
        assertTrue(uiState.userMessage?.contains("Please assign a staff member") == true)

        val report = viewModel.getReportById("REP_001")
        assertEquals(ReportStatus.PENDING, report?.status)
    }

    @Test
    fun assignStaff_attachesStaffToReport() {
        viewModel.assignStaff("REP_001", "John Repair (Electrical)")

        val report = viewModel.getReportById("REP_001")
        assertNotNull(report)
        assertEquals("John Repair (Electrical)", report?.assignedStaffName)
    }

    @Test
    fun updateStatus_afterStaffAssigned_updatesStatusSuccessfully() {
        viewModel.assignStaff("REP_001", "John Repair (Electrical)")
        viewModel.updateStatus("REP_001", ReportStatus.IN_PROGRESS, "Replacing bulb")

        val report = viewModel.getReportById("REP_001")
        assertEquals(ReportStatus.IN_PROGRESS, report?.status)
        assertEquals("John Repair (Electrical)", report?.assignedStaffName)
    }

    @Test
    fun setStatusFilter_filtersReportsByStatus() {
        viewModel.setStatusFilter(ReportStatus.IN_PROGRESS)

        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.filteredReports.size)
        assertEquals("REP_002", uiState.filteredReports[0].reportId)
    }

    @Test
    fun setHostelFilter_filtersReportsByHostel() {
        viewModel.setHostelFilter("Mumba")

        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.filteredReports.size)
        assertEquals("Mumba", uiState.filteredReports[0].hostelName)
    }

    @Test
    fun getStatusHistory_returnsAuditLogs() {
        viewModel.assignStaff("REP_001", "John Repair")
        viewModel.updateStatus("REP_001", ReportStatus.IN_PROGRESS, "Work started")

        val history = viewModel.getStatusHistory("REP_001")
        assertTrue(history.isNotEmpty())
        assertEquals("Work started", history[0].note)
    }
}
