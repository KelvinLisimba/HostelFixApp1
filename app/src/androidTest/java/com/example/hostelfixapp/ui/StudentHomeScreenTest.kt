package com.example.hostelfixapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hostelfixapp.data.preferences.ThemePreferences
import com.example.hostelfixapp.fakes.FakeAuthRepository
import com.example.hostelfixapp.fakes.FakeReportRepository
import com.example.hostelfixapp.ui.student.StudentHomeScreen
import com.example.hostelfixapp.ui.student.StudentViewModel
import com.example.hostelfixapp.ui.theme.HostelFixAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentHomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun studentHomeScreen_rendersHeaderAndFilterButtons() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fakeReportRepo = FakeReportRepository()
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = StudentViewModel(fakeReportRepo, fakeAuthRepo)
        val themePrefs = ThemePreferences(context)

        composeTestRule.setContent {
            HostelFixAppTheme {
                StudentHomeScreen(
                    viewModel = viewModel,
                    studentName = "Alex",
                    profilePicUrl = null,
                    themePreferences = themePrefs,
                    onNavigateToReportProblem = {},
                    onNavigateToReportDetail = {},
                    onNavigateToProfile = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Student Portal", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Welcome", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("All", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Pending", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("In Progress", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Fixed", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Report Problem", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun studentHomeScreen_clickingReportProblemFab_triggersNavigation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fakeReportRepo = FakeReportRepository()
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = StudentViewModel(fakeReportRepo, fakeAuthRepo)
        val themePrefs = ThemePreferences(context)

        var fabClicked = false

        composeTestRule.setContent {
            HostelFixAppTheme {
                StudentHomeScreen(
                    viewModel = viewModel,
                    studentName = "Alex",
                    profilePicUrl = null,
                    themePreferences = themePrefs,
                    onNavigateToReportProblem = { fabClicked = true },
                    onNavigateToReportDetail = {},
                    onNavigateToProfile = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Report Problem", substring = true, useUnmergedTree = true).performClick()
        assertTrue(fabClicked)
    }
}
