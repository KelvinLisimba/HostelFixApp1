package com.example.hostelfixapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.hostelfixapp.data.model.UserRole
import com.example.hostelfixapp.data.preferences.ThemePreferences
import com.example.hostelfixapp.data.repository.AuthRepository
import com.example.hostelfixapp.data.repository.ReportRepository
import com.example.hostelfixapp.ui.auth.AuthViewModel
import com.example.hostelfixapp.ui.auth.ForgotPasswordScreen
import com.example.hostelfixapp.ui.auth.LoginScreen
import com.example.hostelfixapp.ui.auth.SignUpScreen
import com.example.hostelfixapp.ui.profile.ProfileScreen
import com.example.hostelfixapp.ui.staff.MaintenanceRecordsScreen
import com.example.hostelfixapp.ui.staff.StaffDashboardScreen
import com.example.hostelfixapp.ui.staff.StaffReportDetailScreen
import com.example.hostelfixapp.ui.staff.StaffViewModel
import com.example.hostelfixapp.ui.student.ReportDetailScreen
import com.example.hostelfixapp.ui.student.ReportProblemScreen
import com.example.hostelfixapp.ui.student.StudentHomeScreen
import com.example.hostelfixapp.ui.student.StudentViewModel

object HostelFixDestinations {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val FORGOT_PASSWORD = "forgot_password"
    const val PROFILE = "profile"
    const val STUDENT_HOME = "student_home"
    const val REPORT_PROBLEM = "report_problem"
    const val REPORT_DETAIL = "report_detail/{reportId}"
    const val STAFF_DASHBOARD = "staff_dashboard"
    const val STAFF_REPORT_DETAIL = "staff_report_detail/{reportId}"
    const val MAINTENANCE_RECORDS = "maintenance_records"

    fun createReportDetailRoute(reportId: String) = "report_detail/$reportId"
    fun createStaffReportDetailRoute(reportId: String) = "staff_report_detail/$reportId"
}

@Composable
fun HostelFixNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
    themePreferences: ThemePreferences
) {
    val authViewModel = remember { AuthViewModel(authRepository) }
    val studentViewModel = remember { StudentViewModel(reportRepository, authRepository) }
    val staffViewModel = remember { StaffViewModel(reportRepository, authRepository) }

    val startDestination = remember {
        val user = authRepository.getCurrentUser()
        when (user?.role) {
            UserRole.STUDENT -> HostelFixDestinations.STUDENT_HOME
            UserRole.STAFF -> HostelFixDestinations.STAFF_DASHBOARD
            null -> HostelFixDestinations.LOGIN
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(HostelFixDestinations.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = {
                    navController.navigate(HostelFixDestinations.SIGN_UP)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(HostelFixDestinations.FORGOT_PASSWORD)
                },
                onLoginSuccess = { role ->
                    val destination = if (role == UserRole.STUDENT) {
                        HostelFixDestinations.STUDENT_HOME
                    } else {
                        HostelFixDestinations.STAFF_DASHBOARD
                    }
                    navController.navigate(destination) {
                        popUpTo(HostelFixDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(HostelFixDestinations.SIGN_UP) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSignUpSuccess = { role ->
                    val destination = if (role == UserRole.STUDENT) {
                        HostelFixDestinations.STUDENT_HOME
                    } else {
                        HostelFixDestinations.STAFF_DASHBOARD
                    }
                    navController.navigate(destination) {
                        popUpTo(HostelFixDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(HostelFixDestinations.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(HostelFixDestinations.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                themePreferences = themePreferences,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(HostelFixDestinations.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(HostelFixDestinations.STUDENT_HOME) {
            val user = authRepository.getCurrentUser()
            StudentHomeScreen(
                viewModel = studentViewModel,
                studentName = user?.name ?: "Student",
                profilePicUrl = user?.profilePicUrl,
                themePreferences = themePreferences,
                onNavigateToReportProblem = {
                    navController.navigate(HostelFixDestinations.REPORT_PROBLEM)
                },
                onNavigateToReportDetail = { reportId ->
                    navController.navigate(HostelFixDestinations.createReportDetailRoute(reportId))
                },
                onNavigateToProfile = {
                    navController.navigate(HostelFixDestinations.PROFILE)
                },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(HostelFixDestinations.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(HostelFixDestinations.REPORT_PROBLEM) {
            ReportProblemScreen(
                viewModel = studentViewModel,
                onNavigateBack = { navController.popBackStack() },
                onReportSubmitted = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = HostelFixDestinations.REPORT_DETAIL,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            ReportDetailScreen(
                viewModel = studentViewModel,
                reportId = reportId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(HostelFixDestinations.STAFF_DASHBOARD) {
            val user = authRepository.getCurrentUser()
            StaffDashboardScreen(
                viewModel = staffViewModel,
                staffName = user?.name ?: "Staff",
                profilePicUrl = user?.profilePicUrl,
                themePreferences = themePreferences,
                onNavigateToReportDetail = { reportId ->
                    navController.navigate(HostelFixDestinations.createStaffReportDetailRoute(reportId))
                },
                onNavigateToRecords = {
                    navController.navigate(HostelFixDestinations.MAINTENANCE_RECORDS)
                },
                onNavigateToProfile = {
                    navController.navigate(HostelFixDestinations.PROFILE)
                },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(HostelFixDestinations.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = HostelFixDestinations.STAFF_REPORT_DETAIL,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            StaffReportDetailScreen(
                viewModel = staffViewModel,
                reportId = reportId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(HostelFixDestinations.MAINTENANCE_RECORDS) {
            MaintenanceRecordsScreen(
                viewModel = staffViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReportDetail = { reportId ->
                    navController.navigate(HostelFixDestinations.createStaffReportDetailRoute(reportId))
                }
            )
        }
    }
}
