package com.example.hostelfixapp.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HostelFixLocalDbTest {

    private lateinit var db: HostelFixLocalDb

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("test_hostelfix_database.db")
        db = HostelFixLocalDb(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveUser_andRetrieveByEmail() {
        val user = User(
            id = "user_test_01",
            name = "Jane Student",
            email = "jane@example.com",
            studentId = "ST_55",
            role = UserRole.STUDENT,
            assignedHostel = "Mumba",
            assignedRoom = "Room 5"
        )

        db.saveUser(user, "password_hash_123")
        val result = db.getUserByEmail("jane@example.com")

        assertNotNull(result)
        assertEquals("Jane Student", result?.first?.name)
        assertEquals("password_hash_123", result?.second)
    }

    @Test
    fun saveReport_andRetrieveAllReports() {
        val report = MaintenanceReport(
            reportId = "REP_LOCAL_001",
            studentId = "user_test_01",
            studentName = "Jane Student",
            hostelName = "Mumba",
            roomNumber = "Room 5",
            category = ReportCategory.ELECTRICAL,
            description = "Light flickering",
            status = ReportStatus.PENDING,
            assignedStaffName = "John Repair"
        )

        db.saveReport(report)
        val allReports = db.getAllReports()

        assertTrue(allReports.any { it.reportId == "REP_LOCAL_001" })
        val savedReport = allReports.find { it.reportId == "REP_LOCAL_001" }
        assertNotNull(savedReport)
        assertEquals("John Repair", savedReport?.assignedStaffName)
    }
}
