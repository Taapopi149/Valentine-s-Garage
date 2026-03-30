package com.example.valentinesgarage.Screens.Vehicles

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.valentinesgarage.Screens.Mechanic.ServiceTask

// ─── Data ─────────────────────────────────────────────────────────────────────
// TruckDetail combines check-in data with live task data into one model.
// In a real app you'd fetch this from Firestore / Room by truckId.

enum class VehicleCondition(val label: String, val segments: Int) {
    EXCELLENT("Excellent", 4),
    GOOD("Good", 3),
    FAIR("Fair", 2),
    POOR("Poor", 1)
}

data class TruckDetail(
    val id: String,
    val licencePlate: String,
    val driverName: String,
    val odometer: String,
    val condition: VehicleCondition,
    val photoUris: List<Uri> = emptyList(),
    val notes: String = "",
    val status: TruckStatus,
    val tasks: List<ServiceTask> = emptyList()
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckDetailScreen(
    navController: NavController,
    truckId: String?,
    // TODO: fetch real data from your ViewModel / repository by truckId
    truck: TruckDetail = sampleTruck
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = truck.licencePlate,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = truck.driverName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Status badge in the top bar
                    StatusBadgeTruck(truck.status)
                    Spacer(Modifier.width(12.dp))
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Check-in details ──────────────────────────────────────────
            item {
                DetailSection(title = "Check-in details") {
                    InfoRow(label = "Driver",    value = truck.driverName)
                    InfoRow(label = "Odometer",  value = "${truck.odometer} km", mono = true)
                    InfoRow(label = "Condition", value = truck.condition.label)
                    // Condition progress bar
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index < truck.condition.segments) Color(0xFF1A1A1A)
                                        else Color(0xFFEEEEEE)
                                    )
                            )
                        }
                    }
                }
            }

            // ── Photos ────────────────────────────────────────────────────
            if (truck.photoUris.isNotEmpty()) {
                item {
                    SectionLabel("Photos")
                    Spacer(Modifier.height(7.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(truck.photoUris) { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Check-in photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }

            // ── Notes ─────────────────────────────────────────────────────
            if (truck.notes.isNotBlank()) {
                item {
                    DetailSection(title = "Notes / damage") {
                        Text(
                            text = truck.notes,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // ── Task progress ─────────────────────────────────────────────
            item {
                val doneTasks  = truck.tasks.count { it.isDone }
                val totalTasks = truck.tasks.size
                val progress   = if (totalTasks > 0) doneTasks.toFloat() / totalTasks else 0f

                DetailSection(title = "Task progress") {

                    // Progress bar + count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$doneTasks of $totalTasks tasks completed",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFF1A1A1A),
                        trackColor = Color(0xFFEEEEEE)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Task rows — read-only (no checkboxes to tap)
                    truck.tasks.forEach { task ->
                        DetailTaskRow(task = task)
                        if (task != truck.tasks.last()) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Section wrapper ──────────────────────────────────────────────────────────

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(title)
        Spacer(Modifier.height(7.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                content = content
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
}

// ─── Info row ─────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ─── Detail task row (read-only) ──────────────────────────────────────────────

@Composable
fun DetailTaskRow(task: ServiceTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Static checkbox indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (task.isDone) Color(0xFF1A1A1A) else Color.Transparent)
                .then(
                    if (!task.isDone) Modifier.border(
                        width = 1.5.dp,
                        color = Color.LightGray,
                        shape = RoundedCornerShape(4.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (task.isDone) {
                // Checkmark drawn with Canvas-free approach
                Text("✓", color = Color.White, fontSize = 10.sp, lineHeight = 10.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                fontSize = 13.sp,
                color = if (task.isDone)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.isDone) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        task.completedBy?.let { append(it) }
                        task.completedAt?.let { append("  ·  $it") }
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Unclaimed",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Status badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadgeTruck(status: TruckStatus) {
    val (bg, textColor) = when (status) {
        TruckStatus.WAITING     -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
        TruckStatus.IN_PROGRESS -> Color(0xFFFFFBEB) to Color(0xFFB45309)
        TruckStatus.DONE        -> Color(0xFFECFDF5) to Color(0xFF065F46)
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            text = status.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── Sample data (for preview) ────────────────────────────────────────────────

private val sampleTruck = TruckDetail(
    id = "1",
    licencePlate = "N 12345 W",
    driverName = "Johannes Shikongo",
    odometer = "148 302",
    condition = VehicleCondition.GOOD,
    photoUris = emptyList(),
    notes = "Scratches on rear bumper. Small dent on left side panel near the door.",
    status = TruckStatus.IN_PROGRESS,
    tasks = listOf(
        ServiceTask(
            "t1",
            "Oil & filter change",
            isDone = true,
            completedBy = "David M.",
            completedAt = "09:12",
            note = "Used 10W-40 synthetic"
        ),
        ServiceTask("t2", "Brake inspection",        isDone = true,  completedBy = "Aina N.",   completedAt = "09:45", note = "Front pads replaced"),
        ServiceTask("t3", "Tyre pressure check",     isDone = false),
        ServiceTask("t4", "Coolant top-up",          isDone = false),
        ServiceTask("t5", "Electrical system check", isDone = false),
    )
)

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun TruckDetailPreview() {
    TruckDetailScreen(navController = rememberNavController(), truckId = "1")
}