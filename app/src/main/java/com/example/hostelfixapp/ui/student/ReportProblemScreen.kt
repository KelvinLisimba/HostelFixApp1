package com.example.hostelfixapp.ui.student

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hostelfixapp.data.model.ReportCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportProblemScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    onReportSubmitted: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedHostel by remember { mutableStateOf("") }
    var selectedRoom by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReportCategory.ELECTRICAL) }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    var hostelDropdownExpanded by remember { mutableStateOf(false) }
    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoUri = uri
    }

    val activeRooms = remember(selectedHostel) {
        uiState.availableHostels.find { it.name == selectedHostel }?.rooms ?: emptyList()
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            if (msg.contains("successfully", ignoreCase = true)) {
                showSuccessDialog = true
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
                }
            }
            viewModel.clearUserMessage()
        }
    }

    // Green Success Confirmation Popup Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onReportSubmitted()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Report Submitted Successfully!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            },
            text = {
                Text(
                    text = "Your maintenance complaint has been submitted and sent to the maintenance office. You can track its progress on your dashboard.",
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onReportSubmitted()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View My Reports", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFFE8F5E9)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Maintenance Problem", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Hostel & Room Location
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "1. Location Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hostel Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedHostel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Hostel Building") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hostelDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { hostelDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = hostelDropdownExpanded,
                            onDismissRequest = { hostelDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            uiState.availableHostels.forEach { hostel ->
                                DropdownMenuItem(
                                    text = { Text(hostel.name) },
                                    onClick = {
                                        selectedHostel = hostel.name
                                        selectedRoom = ""
                                        hostelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Room Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedRoom,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Room Number") },
                            leadingIcon = { Icon(Icons.Default.DoorFront, contentDescription = null) },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            enabled = selectedHostel.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = selectedHostel.isNotBlank()) {
                                    roomDropdownExpanded = true
                                },
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (selectedHostel.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { roomDropdownExpanded = true }
                            )
                        }

                        DropdownMenu(
                            expanded = roomDropdownExpanded,
                            onDismissRequest = { roomDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            activeRooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room) },
                                    onClick = {
                                        selectedRoom = room
                                        roomDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Category Selector
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "2. Problem Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportCategory.entries.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = {
                                    Text(
                                        text = category.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    when (category) {
                                        ReportCategory.ELECTRICAL -> Icon(
                                            Icons.Default.ElectricBolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        ReportCategory.PLUMBING -> Icon(
                                            Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        ReportCategory.DOORS_WINDOWS -> Icon(
                                            Icons.Default.DoorFront,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        else -> null
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Problem Description & Photo Upload
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "3. Problem Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the issue in detail") },
                        placeholder = { Text("e.g. Light tube flickering, tap leaking, broken lock...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Attach Photo (Optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (photoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Attached Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = { photoUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo")
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add Photo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to upload image from gallery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    viewModel.submitReport(
                        hostelName = selectedHostel,
                        roomNumber = selectedRoom,
                        category = selectedCategory,
                        description = description,
                        photoUri = photoUri,
                        onSuccess = {
                            // Dialog will be shown via LaunchedEffect(uiState.userMessage)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit Maintenance Report", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
