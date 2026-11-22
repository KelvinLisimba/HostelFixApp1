package com.example.hostelfixapp.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.example.hostelfixapp.data.model.ReportStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffReportDetailScreen(
    viewModel: StaffViewModel,
    reportId: String,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val report = remember(uiState.allReports, reportId) {
        viewModel.getReportById(reportId)
    }

    val historyList = remember(report) {
        report?.let { viewModel.getStatusHistory(it.reportId) } ?: emptyList()
    }

    var staffDropdownExpanded by remember { mutableStateOf(false) }
    var updateNote by remember { mutableStateOf("") }

    // Confirmation Dialog state
    var pendingStatusUpdate by remember { mutableStateOf<ReportStatus?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val staffMembersList = listOf("John Repair (Electrical)", "Sarah Plumber (Plumbing)", "David Carpenter (Furniture)", "Office Desk Staff")

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
            }
            viewModel.clearUserMessage()
        }
    }

    // Confirmation Dialog Popup
    if (showConfirmDialog && pendingStatusUpdate != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Status Update", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to update this complaint status to '${pendingStatusUpdate!!.displayName}'?")
                    if (updateNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Maintenance Note provided:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text("\"$updateNote\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No maintenance note entered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        report?.let {
                            viewModel.updateStatus(it.reportId, pendingStatusUpdate!!, updateNote)
                        }
                        updateNote = ""
                        showConfirmDialog = false
                        pendingStatusUpdate = null
                        onNavigateBack()
                    }
                ) {
                    Text("Confirm & Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Maintenance Complaint", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Report not found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Action Box: Assign Staff & Update Status Pipeline
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Staff Management Panel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (report.assignedStaffName.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF3E0)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚠️ Unassigned Complaint: Please assign a staff member below before moving status to In Progress or Fixed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Staff Assignment Dropdown
                        Text(
                            text = "Assign Staff Member:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box {
                            OutlinedButton(
                                onClick = { staffDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AssignmentInd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = report.assignedStaffName ?: "Select Staff Member to Assign",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = staffDropdownExpanded,
                                onDismissRequest = { staffDropdownExpanded = false }
                            ) {
                                staffMembersList.forEach { staffName ->
                                    DropdownMenuItem(
                                        text = { Text(staffName) },
                                        onClick = {
                                            viewModel.assignStaff(report.reportId, staffName)
                                            staffDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Update Status Action
                        Text(
                            text = "Update Complaint Status Pipeline:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = updateNote,
                            onValueChange = { updateNote = it },
                            label = { Text("Maintenance note (optional)") },
                            placeholder = { Text("e.g. Parts replaced, work in progress...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (report.assignedStaffName.isNullOrBlank()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "⚠️ Please assign a staff member before setting status to In Progress!",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        staffDropdownExpanded = true
                                    } else {
                                        pendingStatusUpdate = ReportStatus.IN_PROGRESS
                                        showConfirmDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0277BD)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("In Progress", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (report.assignedStaffName.isNullOrBlank()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "⚠️ Please assign a staff member before marking as Fixed!",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        staffDropdownExpanded = true
                                    } else {
                                        pendingStatusUpdate = ReportStatus.FIXED
                                        showConfirmDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Mark Fixed", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // History Audit Log Card (Moved Higher Up for Prominent Visibility)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Maintenance History & Audit Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        historyList.forEach { history ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${history.newStatus.displayName} • ${history.updatedByStaffName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = history.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormat.format(Date(history.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                // Complaint Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Location & Student Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        StaffDetailRow(label = "Hostel Building", value = report.hostelName)
                        StaffDetailRow(label = "Room Number", value = report.roomNumber)
                        StaffDetailRow(label = "Student Name", value = report.studentName)
                        StaffDetailRow(label = "Category", value = report.category.displayName)
                        StaffDetailRow(label = "Current Status", value = report.status.displayName)
                        StaffDetailRow(label = "Date Reported", value = dateFormat.format(Date(report.createdAt)))

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Problem Description",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Student Photo Attachment",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!report.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = report.photoUrl,
                                contentDescription = "Student attachment",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "No photo attached",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
