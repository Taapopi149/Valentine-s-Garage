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
import androidx.compose.runtime.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.valentinesgarage.Data.DAO.NotesDao
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.Entities.Tasks
import com.example.valentinesgarage.Data.Entities.Truck
import com.example.valentinesgarage.Screens.CheckIn.VehicleCondition
import com.example.valentinesgarage.Screens.Employee.ViewModelFactory.TruckDetailViewModelFactory
import com.example.valentinesgarage.Screens.Mechanic.ServiceTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

// ─── UI model ─────────────────────────────────────────────────────────────────

data class TruckDetail(
    val id:           Int,
    val licencePlate: String,
    val driverName:   String,
    val odometer:     String,
    val condition:    VehicleCondition,
    val photoUris:    List<Uri> = emptyList(),
    val notes:        String = "",
    val status:       TruckStatus,
    val tasks:        List<ServiceTask> = emptyList()
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class TruckDetailViewModel(
    private val truckDao: TruckDao,
    private val tasksDao: TasksDao,
    private val notesDao: NotesDao,
    private val truckId:  Int
) : ViewModel() {

    private val _detail    = MutableStateFlow<TruckDetail?>(null)
    val detail: StateFlow<TruckDetail?> = _detail.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // Combine truck row + its tasks into one flow so both
            // update the UI whenever either changes
            combine(
                truckDao.getTruckById(truckId).filterNotNull(),
                tasksDao.getTasksForTruck(truckId)
            ) { truck, tasks ->
                val note = notesDao.getNoteForTruck(truckId) ?: ""
                truck.toDetail(tasks, note)
            }.collect { detail ->
                _detail.value    = detail
                _isLoading.value = false
            }
        }
    }
}

// ─── Mappers ──────────────────────────────────────────────────────────────────

private fun Truck.toDetail(tasks: List<Tasks>, note: String) = TruckDetail(
    id           = truckId,
    licencePlate = licencePlate,
    driverName   = DriverName,
    odometer     = Odmeter.toString(),
    condition    = Condition,
    // Parse the "|"-separated URI string stored in the DB back to a list
    photoUris    = photoUris
        .split("|")
        .filter { it.isNotBlank() }
        .map { Uri.parse(it) },
    notes        = note,
    status       = truckStatus,
    tasks        = tasks.map { it.toServiceTask() }
)

private fun Tasks.toServiceTask() = ServiceTask(
    id          = Taskid,
    name        = description,
    isDone      = status == "Done",
    completedBy = completedBy,
    completedAt = completedAt,
    note        = note
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckDetailScreen(
    navController: NavController,
    truckId:       Int?,
    truckDao:      TruckDao,
    tasksDao:      TasksDao,
    notesDao:      NotesDao
) {
    // Guard against null truckId — shouldn't happen but nav args can be null
    if (truckId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Truck not found")
        }
        return
    }

    val viewModel: TruckDetailViewModel = viewModel(
        factory = TruckDetailViewModelFactory(truckDao, tasksDao, notesDao, truckId)
    )

    val detail    by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // ── Loading ───────────────────────────────────────────────────────────────
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF1A1A1A))
        }
        return
    }

    // ── Not found ─────────────────────────────────────────────────────────────
    if (detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Truck not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val truck = detail!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = truck.licencePlate,
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text     = truck.driverName,
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    StatusBadgeTruck(truck.status)
                    Spacer(Modifier.width(12.dp))
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier        = Modifier.padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Check-in details ──────────────────────────────────────────
            item {
                DetailSection(title = "Check-in details") {
                    InfoRow(label = "Driver",    value = truck.driverName)
                    InfoRow(label = "Odometer",  value = "${truck.odometer} km", mono = true)
                    InfoRow(label = "Condition", value = truck.condition.label)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index < truck.condition.filledSegments) Color(0xFF1A1A1A)
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
                                painter            = rememberAsyncImagePainter(uri),
                                contentDescription = "Check-in photo",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
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
                            text      = truck.notes,
                            fontSize  = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$doneTasks of $totalTasks tasks completed",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress   = { progress },
                        modifier   = Modifier.fillMaxWidth().height(4.dp),
                        color      = Color(0xFF1A1A1A),
                        trackColor = Color(0xFFEEEEEE)
                    )
                    Spacer(Modifier.height(8.dp))

                    if (truck.tasks.isEmpty()) {
                        Text(
                            "No tasks assigned yet",
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        truck.tasks.forEach { task ->
                            DetailTaskRow(task = task)
                            if (task != truck.tasks.last()) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
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
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(12.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                content  = content
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Medium,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
}

// ─── Info row ─────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text       = value,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ─── Detail task row (read-only) ──────────────────────────────────────────────

@Composable
fun DetailTaskRow(task: ServiceTask) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                Text("✓", color = Color.White, fontSize = 10.sp, lineHeight = 10.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text           = task.name,
                fontSize       = 13.sp,
                color          = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isDone) TextDecoration.LineThrough
                else TextDecoration.None
            )
            if (task.isDone) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (task.completedBy.isNotBlank()) append(task.completedBy)
                        if (task.completedAt.isNotBlank()) append("  ·  ${task.completedAt}")
                    },
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.note.isNotBlank()) {
                    Text(
                        text      = task.note,
                        fontSize  = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    // Show assignee name if available, fallback to Unassigned
                    text     = if (task.assignedTo.isNotBlank() && task.assignedTo != "Unassigned")
                        "Assigned to ${task.assignedTo}"
                    else "Unassigned",
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
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
            text       = status.label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            color      = textColor,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun TruckDetailPreview() {
    MaterialTheme {
        val fakeTruck = TruckDetail(
            id           = 1,
            licencePlate = "N 12345 W",
            driverName   = "Johannes Shikongo",
            odometer     = "148302",
            condition    = VehicleCondition.GOOD,
            photoUris    = emptyList(),
            notes        = "Scratches on rear bumper. Small dent on left side panel.",
            status       = TruckStatus.IN_PROGRESS,
            tasks        = listOf(
                ServiceTask(1, "Oil & filter change", assignedTo = "David M.",  isDone = true,  completedBy = "David M.", completedAt = "09:12", note = "Used 10W-40"),
                ServiceTask(2, "Brake inspection",    assignedTo = "Aina N.",   isDone = true,  completedBy = "Aina N.",  completedAt = "09:45", note = "Front pads replaced"),
                ServiceTask(3, "Tyre pressure check", assignedTo = "Simon S.",  isDone = false),
                ServiceTask(4, "Coolant top-up",      assignedTo = "David M.",  isDone = false),
            )
        )
        Scaffold { padding ->
            LazyColumn(
                modifier        = Modifier.padding(padding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DetailSection("Check-in details") {
                        InfoRow("Driver",    fakeTruck.driverName)
                        InfoRow("Odometer",  "${fakeTruck.odometer} km", mono = true)
                        InfoRow("Condition", fakeTruck.condition.label)
                    }
                }
                item {
                    val done     = fakeTruck.tasks.count { it.isDone }
                    val total    = fakeTruck.tasks.size
                    val progress = done.toFloat() / total
                    DetailSection("Task progress") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$done of $total tasks completed", fontSize = 11.sp)
                            Text("${(progress * 100).toInt()}%", fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress   = { progress },
                            modifier   = Modifier.fillMaxWidth().height(4.dp),
                            color      = Color(0xFF1A1A1A),
                            trackColor = Color(0xFFEEEEEE)
                        )
                        Spacer(Modifier.height(8.dp))
                        fakeTruck.tasks.forEach { task ->
                            DetailTaskRow(task = task)
                            if (task != fakeTruck.tasks.last()) HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}