package com.example.hostelfixapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hostelfixapp.data.preferences.ThemePreferences
import com.example.hostelfixapp.fakes.FakeAuthRepository
import com.example.hostelfixapp.fakes.FakeReportRepository
import com.example.hostelfixapp.ui.staff.StaffDashboardScreen
import com.example.hostelfixapp.ui.staff.StaffViewModel
import com.example.hostelfixapp.ui.theme.HostelFixAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaffDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun staffDashboardScreen_rendersHeaderAndStatusFilterButtons() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fakeReportRepo = FakeReportRepository()
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = StaffViewModel(fakeReportRepo, fakeAuthRepo)
        val themePrefs = ThemePreferences(context)

        composeTestRule.setContent {
            HostelFixAppTheme {
                StaffDashboardScreen(
                    viewModel = viewModel,
                    staffName = "John Maintenance",
                    profilePicUrl = null,
                    themePreferences = themePrefs,
                    onNavigateToReportDetail = {},
                    onNavigateToRecords = {},
                    onNavigateToProfile = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Maintenance Office Portal").assertExists()
        composeTestRule.onNodeWithText("Logged in: John Maintenance").assertExists()
        composeTestRule.onNodeWithText("Submitted Complaints").assertExists()
        composeTestRule.onNodeWithText("All (0)").assertExists()
        composeTestRule.onNodeWithText("Pending (0)").assertExists()
        composeTestRule.onNodeWithText("In Progress (0)").assertExists()
        composeTestRule.onNodeWithText("Fixed (0)").assertExists()
    }
}
