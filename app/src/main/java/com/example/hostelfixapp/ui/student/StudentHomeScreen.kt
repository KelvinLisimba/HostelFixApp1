package com.example.hostelfixapp.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.hostelfixapp.ui.components.verticalScrollbar
import com.example.hostelfixapp.ui.components.FilterRowScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.preferences.AppThemeMode
import com.example.hostelfixapp.data.preferences.ThemePreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    viewModel: StudentViewModel,
    studentName: String,
    profilePicUrl: String?,
    themePreferences: ThemePreferences,
    onNavigateToReportProblem: () -> Unit,
    onNavigateToReportDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by themePreferences.themeMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedStatusFilter by remember { mutableStateOf<ReportStatus?>(null) } // null = All, PENDING, IN_PROGRESS, FIXED
    var searchQuery by remember { mutableStateOf("") }
    var showArchiveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStudentData()
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            }
            viewModel.clearUserMessage()
        }
    }

    val pendingCount = uiState.studentReports.count { it.status == ReportStatus.PENDING }
    val inProgressCount = uiState.studentReports.count { it.status == ReportStatus.IN_PROGRESS }
    val fixedCount = uiState.studentReports.count { it.status == ReportStatus.FIXED }

    val displayedList = uiState.studentReports.filter { report ->
        val matchesStatus = selectedStatusFilter == null || report.status == selectedStatusFilter
        val query = searchQuery.trim().lowercase()
        val matchesQuery = query.isEmpty() ||
                report.roomNumber.lowercase().contains(query) ||
                report.hostelName.lowercase().contains(query) ||
                report.category.displayName.lowercase().contains(query) ||
                report.description.lowercase().contains(query)
        matchesStatus && matchesQuery
    }

    // Archive Dialog Popup
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("📦 Archived Reports", fontWeight = FontWeight.Bold) },
            text = {
                if (uiState.archivedReports.isEmpty()) {
                    Text("No archived reports found. Fixed reports older than 5 minutes appear here.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.archivedReports.forEach { archivedReport ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showArchiveDialog = false
                                        onNavigateToReportDetail(archivedReport.reportId)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${archivedReport.hostelName} - ${archivedReport.roomNumber}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = if (archivedReport.isConfirmedByStudent) "Student Confirmed" else "Archived Fixed",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Category: ${archivedReport.category.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (!archivedReport.assignedStaffName.isNullOrBlank()) {
                                        Text(
                                            text = "Technician: ${archivedReport.assignedStaffName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Move Report to Active Dashboard Action
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.unarchiveReport(archivedReport.reportId)
                                            showArchiveDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Move to Active Dashboard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToProfile() }
                            .padding(4.dp)
                    ) {
                        // Profile Logo on Left Side
                        if (!profilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Avatar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Text on Right Side of Logo
                        Column {
                            Text(
                                text = "Student Portal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to view profile ($studentName) ➔",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Theme Toggle Button (Light/Dark mode)
                    IconButton(onClick = { themePreferences.toggleLightDark() }) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    // Archive Logs Dropdown Button
                    OutlinedButton(
                        onClick = { showArchiveDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Archived Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToReportProblem,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Report Problem", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search room #, category, or description...", fontSize = 15.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(24.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(24.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Prominent, Large, Tactile Filter Buttons Row
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = {
                            Text(
                                text = "All (${uiState.studentReports.size})",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.height(48.dp)
                    )

                    FilterChip(
                        selected = selectedStatusFilter == ReportStatus.PENDING,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == ReportStatus.PENDING) null else ReportStatus.PENDING
                        },
                        label = {
                            Text(
                                text = "Pending ($pendingCount)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFFFF3E0),
                            labelColor = Color(0xFFE65100),
                            selectedContainerColor = Color(0xFFFFB74D),
                            selectedLabelColor = Color(0xFF331700)
                        ),
                        modifier = Modifier.height(48.dp)
                    )

                    FilterChip(
                        selected = selectedStatusFilter == ReportStatus.IN_PROGRESS,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == ReportStatus.IN_PROGRESS) null else ReportStatus.IN_PROGRESS
                        },
                        label = {
                            Text(
                                text = "In Progress ($inProgressCount)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFE1F5FE),
                            labelColor = Color(0xFF0277BD),
                            selectedContainerColor = Color(0xFF4FC3F7),
                            selectedLabelColor = Color(0xFF001F31)
                        ),
                        modifier = Modifier.height(48.dp)
                    )

                    FilterChip(
                        selected = selectedStatusFilter == ReportStatus.FIXED,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == ReportStatus.FIXED) null else ReportStatus.FIXED
                        },
                        label = {
                            Text(
                                text = "Fixed ($fixedCount)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFE8F5E9),
                            labelColor = Color(0xFF2E7D32),
                            selectedContainerColor = Color(0xFF81C784),
                            selectedLabelColor = Color(0xFF062208)
                        ),
                        modifier = Modifier.height(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (displayedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No maintenance reports found for this filter.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedList, key = { it.reportId }) { report ->
                            ReportItemCard(
                                report = report,
                                onClick = { onNavigateToReportDetail(report.reportId) },
                                isOnline = uiState.isOnline
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun ReportItemCard(
    report: MaintenanceReport,
    onClick: () -> Unit,
    isOnline: Boolean = true
) {
    val categoryIcon: ImageVector = when (report.category) {
        ReportCategory.ELECTRICAL -> Icons.Default.ElectricBolt
        ReportCategory.PLUMBING -> Icons.Default.WaterDrop
        ReportCategory.DOORS_WINDOWS -> Icons.Default.DoorFront
        else -> Icons.Default.Build
    }

    val (statusColor, statusBg) = when (report.status) {
        ReportStatus.PENDING -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
        ReportStatus.IN_PROGRESS -> Pair(Color(0xFF0277BD), Color(0xFFE1F5FE))
        ReportStatus.FIXED -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(report.createdAt) { dateFormat.format(Date(report.createdAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${report.hostelName} • ${report.roomNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Category: ${report.category.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = report.status.displayName,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Dedicated Technician Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = if (!report.assignedStaffName.isNullOrBlank()) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Technician: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (!report.assignedStaffName.isNullOrBlank()) report.assignedStaffName else "Unassigned",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (!report.assignedStaffName.isNullOrBlank()) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated Date & Chevron Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reported: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
