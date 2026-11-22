package com.example.hostelfixapp.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportRepositoryImplIntegrationTest {

    private lateinit var repository: ReportRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("hostelfix_database.db")
        repository = ReportRepositoryImpl(context)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("hostelfix_database.db")
    }

    @Test
    fun submitReport_persistsToRealSQLiteDatabase_andEmitsInFlow(): Unit = runBlocking {
        val report = MaintenanceReport(
            studentId = "STUDENT_INTEGRATION_01",
            studentName = "Alice Integration",
            hostelName = "Kenyatta",
            roomNumber = "Room 15",
            category = ReportCategory.ELECTRICAL,
            description = "Light flickering in hallway"
        )

        val result = repository.submitReport(report)
        assertTrue(result.isSuccess)

        val reportsFromDb = repository.getStudentReportsFlow("STUDENT_INTEGRATION_01").first()
        assertEquals(1, reportsFromDb.size)
        assertEquals("Alice Integration", reportsFromDb[0].studentName)
        assertEquals(ReportStatus.PENDING, reportsFromDb[0].status)
    }

    @Test
    fun assignStaff_andUpdateStatus_persistsStatusPipelineInSQLite(): Unit = runBlocking {
        val report = MaintenanceReport(
            studentId = "STUDENT_INTEGRATION_02",
            studentName = "Bob",
            hostelName = "Mumba",
            roomNumber = "Room 02",
            category = ReportCategory.PLUMBING,
            description = "Sink leaking"
        )
        val submitResult = repository.submitReport(report)
        val submittedReport = submitResult.getOrThrow()

        // 1. Assign Staff
        val assignResult = repository.assignStaff(submittedReport.reportId, "STAFF_99", "John Plumber")
        assertTrue(assignResult.isSuccess)

        // 2. Move to IN_PROGRESS
        val statusResult = repository.updateReportStatus(submittedReport.reportId, ReportStatus.IN_PROGRESS, "STAFF_99", "John Plumber", "Parts replaced")
        assertTrue(statusResult.isSuccess)

        // 3. Verify real SQLite DB reflects assignment and status update
        val updatedReport = repository.getReportById(submittedReport.reportId)
        assertNotNull(updatedReport)
        assertEquals(ReportStatus.IN_PROGRESS, updatedReport?.status)
        assertEquals("John Plumber", updatedReport?.assignedStaffName)
    }
}
