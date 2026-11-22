package com.example.hostelfixapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hostelfixapp.fakes.FakeAuthRepository
import com.example.hostelfixapp.ui.auth.AuthViewModel
import com.example.hostelfixapp.ui.auth.LoginScreen
import com.example.hostelfixapp.ui.theme.HostelFixAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_rendersAllEssentialUiElements() {
        val fakeAuthRepository = FakeAuthRepository()
        val authViewModel = AuthViewModel(fakeAuthRepository)

        composeTestRule.setContent {
            HostelFixAppTheme {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignUp = {},
                    onNavigateToForgotPassword = {},
                    onLoginSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("HostelFix", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Sign In", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun loginScreen_allowsTypingEmailAndPassword() {
        val fakeAuthRepository = FakeAuthRepository()
        val authViewModel = AuthViewModel(fakeAuthRepository)

        composeTestRule.setContent {
            HostelFixAppTheme {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignUp = {},
                    onNavigateToForgotPassword = {},
                    onLoginSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Email", substring = true, useUnmergedTree = true).performTextInput("student@example.com")
        composeTestRule.onNodeWithText("Password", substring = true, useUnmergedTree = true).performTextInput("secure123")

        composeTestRule.onNodeWithText("student@example.com", substring = true, useUnmergedTree = true).assertExists()
    }
}
