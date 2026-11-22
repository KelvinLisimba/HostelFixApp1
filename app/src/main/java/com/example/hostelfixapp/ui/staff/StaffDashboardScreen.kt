package com.example.hostelfixapp.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.example.hostelfixapp.ui.components.verticalScrollbar
import com.example.hostelfixapp.ui.components.FilterRowScrollbar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
fun StaffDashboardScreen(
    viewModel: StaffViewModel,
    staffName: String,
    profilePicUrl: String?,
    themePreferences: ThemePreferences,
    onNavigateToReportDetail: (String) -> Unit,
    onNavigateToRecords: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by themePreferences.themeMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var hostelDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadStaffData()
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

    val fiveMinutesInMillis = 5 * 60 * 1000L
    val currentTime = System.currentTimeMillis()

    val activeReportsForStats = uiState.allReports.filter { report ->
        val matchesHostel = (uiState.selectedHostelFilter == null || report.hostelName == uiState.selectedHostelFilter)
        val isArchivedFixed = report.status == ReportStatus.FIXED &&
                (currentTime - report.updatedAt) > fiveMinutesInMillis
        matchesHostel && !isArchivedFixed
    }

    val pending = activeReportsForStats.count { it.status == ReportStatus.PENDING }
    val inProg = activeReportsForStats.count { it.status == ReportStatus.IN_PROGRESS }
    val fixed = activeReportsForStats.count { it.status == ReportStatus.FIXED }

    val displayedReports = uiState.filteredReports.filter { report ->
        val query = searchQuery.trim().lowercase()
        query.isEmpty() ||
                report.roomNumber.lowercase().contains(query) ||
                report.hostelName.lowercase().contains(query) ||
                report.category.displayName.lowercase().contains(query) ||
                report.description.lowercase().contains(query) ||
                report.studentName.lowercase().contains(query)
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
                                text = "Maintenance Office Portal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to view profile ➔",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Theme Toggle Button
                    IconButton(onClick = { themePreferences.toggleLightDark() }) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    // Completed Records Dropdown Button
                    OutlinedButton(
                        onClick = onNavigateToRecords,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Records Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                // Header Row: Title & Hostel Filter Dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Submitted Complaints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Hostel Filter Dropdown
                    Box {
                        OutlinedButton(
                            onClick = { hostelDropdownExpanded = true },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = uiState.selectedHostelFilter ?: "All Hostels",
                                fontSize = 12.sp
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = hostelDropdownExpanded,
                            onDismissRequest = { hostelDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Hostels") },
                                onClick = {
                                    viewModel.setHostelFilter(null)
                                    hostelDropdownExpanded = false
                                }
                            )
                            uiState.availableHostels.forEach { hostel ->
                                DropdownMenuItem(
                                    text = { Text(hostel.name) },
                                    onClick = {
                                        viewModel.setHostelFilter(hostel.name)
                                        hostelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search room, student, category, or detail...", fontSize = 15.5.sp) },
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
                        selected = uiState.selectedStatusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = {
                            Text(
                                text = "All (${activeReportsForStats.size})",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.height(48.dp)
                    )

                    FilterChip(
                        selected = uiState.selectedStatusFilter == ReportStatus.PENDING,
                        onClick = {
                            val newFilter = if (uiState.selectedStatusFilter == ReportStatus.PENDING) null else ReportStatus.PENDING
                            viewModel.setStatusFilter(newFilter)
                        },
                        label = {
                            Text(
                                text = "Pending ($pending)",
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
                        selected = uiState.selectedStatusFilter == ReportStatus.IN_PROGRESS,
                        onClick = {
                            val newFilter = if (uiState.selectedStatusFilter == ReportStatus.IN_PROGRESS) null else ReportStatus.IN_PROGRESS
                            viewModel.setStatusFilter(newFilter)
                        },
                        label = {
                            Text(
                                text = "In Progress ($inProg)",
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
                        selected = uiState.selectedStatusFilter == ReportStatus.FIXED,
                        onClick = {
                            val newFilter = if (uiState.selectedStatusFilter == ReportStatus.FIXED) null else ReportStatus.FIXED
                            viewModel.setStatusFilter(newFilter)
                        },
                        label = {
                            Text(
                                text = "Fixed ($fixed)",
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

                if (displayedReports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No maintenance complaints found for this filter.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedReports, key = { it.reportId }) { report ->
                            StaffReportCard(
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
fun StaffReportCard(
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
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
                            text = "${report.hostelName} - ${report.roomNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Student: ${report.studentName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Category: ${report.category.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Dedicated Technician Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = if (report.assignedStaffName != null) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Technician: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.assignedStaffName ?: "Unassigned",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (report.assignedStaffName != null) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated Date Line
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
                        text = dateFormat.format(Date(report.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Manage",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
