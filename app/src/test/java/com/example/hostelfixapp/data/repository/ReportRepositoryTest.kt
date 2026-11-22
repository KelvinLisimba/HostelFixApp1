package com.example.hostelfixapp.data.repository

import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.fakes.FakeReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportRepositoryTest {

    private lateinit var repository: FakeReportRepository

    @Before
    fun setUp() {
        repository = FakeReportRepository()
    }

    @Test
    fun submitReport_addsReportToFlowAndReturnsSuccess() = runTest {
        val report = MaintenanceReport(
            studentId = "ST_100",
            studentName = "Alice",
            hostelName = "Kenyatta",
            roomNumber = "Room 12",
            category = ReportCategory.PLUMBING,
            description = "Pipe leak"
        )

        val result = repository.submitReport(report)
        assertTrue(result.isSuccess)

        val allReports = repository.getAllReportsFlow().first()
        assertEquals(1, allReports.size)
        assertEquals("Alice", allReports[0].studentName)
        assertEquals(ReportStatus.PENDING, allReports[0].status)
    }

    @Test
    fun assignStaff_attachesTechnicianToReport() = runTest {
        val report = MaintenanceReport(
            reportId = "REP_55",
            studentId = "ST_100",
            studentName = "Alice",
            hostelName = "Kenyatta",
            roomNumber = "Room 12",
            category = ReportCategory.ELECTRICAL,
            description = "Power outlet broken"
        )
        repository.submitReport(report)

        val assignResult = repository.assignStaff("REP_55", "STAFF_01", "John Electrician")
        assertTrue(assignResult.isSuccess)

        val updatedReport = repository.getReportById("REP_55")
        assertNotNull(updatedReport)
        assertEquals("John Electrician", updatedReport?.assignedStaffName)
    }

    @Test
    fun updateReportStatus_updatesStatusAndRecordsHistory() = runTest {
        val report = MaintenanceReport(
            reportId = "REP_88",
            studentId = "ST_100",
            studentName = "Alice",
            hostelName = "Mumba",
            roomNumber = "Room 04",
            category = ReportCategory.DOORS_WINDOWS,
            description = "Lock jammed"
        )
        repository.submitReport(report)

        repository.assignStaff("REP_88", "STAFF_02", "David Carpenter")
        val updateResult = repository.updateReportStatus("REP_88", ReportStatus.IN_PROGRESS, "STAFF_02", "David Carpenter", "Fixing lock")

        assertTrue(updateResult.isSuccess)
        val updatedReport = repository.getReportById("REP_88")
        assertEquals(ReportStatus.IN_PROGRESS, updatedReport?.status)

        val history = repository.getStatusHistory("REP_88")
        assertEquals(1, history.size)
        assertEquals(ReportStatus.IN_PROGRESS, history[0].newStatus)
        assertEquals("Fixing lock", history[0].note)
    }

    @Test
    fun confirmResolution_marksReportAsConfirmedByStudent() = runTest {
        val report = MaintenanceReport(
            reportId = "REP_99",
            studentId = "ST_100",
            studentName = "Alice",
            hostelName = "Kachere",
            roomNumber = "Room 01",
            category = ReportCategory.PLUMBING,
            description = "Tap leaking"
        )
        repository.submitReport(report)

        val confirmResult = repository.confirmResolution("REP_99")
        assertTrue(confirmResult.isSuccess)

        val updatedReport = repository.getReportById("REP_99")
        assertTrue(updatedReport?.isConfirmedByStudent == true)
    }

    @Test
    fun getStudentReportsFlow_filtersReportsByStudentId() = runTest {
        val report1 = MaintenanceReport(reportId = "R1", studentId = "STUDENT_A", studentName = "Alice")
        val report2 = MaintenanceReport(reportId = "R2", studentId = "STUDENT_B", studentName = "Bob")

        repository.submitReport(report1)
        repository.submitReport(report2)

        val aliceReports = repository.getStudentReportsFlow("STUDENT_A").first()
        assertEquals(1, aliceReports.size)
        assertEquals("R1", aliceReports[0].reportId)
    }
}
