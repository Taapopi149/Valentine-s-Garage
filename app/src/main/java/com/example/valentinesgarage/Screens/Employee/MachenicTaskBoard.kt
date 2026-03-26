package com.example.valentinesgarage.Screens.Mechanic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ─── Data ─────────────────────────────────────────────────────────────────────

enum class TruckStatus(val label: String) {
    WAITING("Waiting"),
    IN_PROGRESS("In Progress"),
    DONE("Done")
}

data class ServiceTask(
    val id: String,
    val name: String,
    val isDone: Boolean = false,
    val completedBy: String? = null,
    val completedAt: String? = null,
    val note: String = ""
)

data class TruckJob(
    val id: String,
    val licencePlate: String,
    val driverName: String,
    val status: TruckStatus,
    val tasks: List<ServiceTask>
)

// ─── Shared ViewModel ─────────────────────────────────────────────────────────
// Both screens share this ViewModel so task updates are reflected everywhere.

class MechanicViewModel : ViewModel() {

    // TODO: replace with Firestore / Room live data
    var jobs by mutableStateOf(
        listOf(
            TruckJob(
                id = "1",
                licencePlate = "N 12345 W",
                driverName = "Johannes Shikongo",
                status = TruckStatus.IN_PROGRESS,
                tasks = listOf(
                    ServiceTask("t1", "Oil & filter change",  isDone = true,  completedBy = "David M.",  completedAt = "09:12", note = "Used 10W-40 synthetic"),
                    ServiceTask("t2", "Brake inspection",     isDone = true,  completedBy = "Aina N.",   completedAt = "09:45", note = "Front pads replaced"),
                    ServiceTask("t3", "Tyre pressure check"),
                    ServiceTask("t4", "Coolant top-up"),
                    ServiceTask("t5", "Electrical system check"),
                )
            ),
            TruckJob(
                id = "2",
                licencePlate = "N 78900 W",
                driverName = "Petrus Hamutenya",
                status = TruckStatus.WAITING,
                tasks = listOf(
                    ServiceTask("t6",  "Full service inspection"),
                    ServiceTask("t7",  "Oil change"),
                    ServiceTask("t8",  "Tyre rotation"),
                )
            ),
            TruckJob(
                id = "3",
                licencePlate = "N 55231 W",
                driverName = "Maria Shipanga",
                status = TruckStatus.DONE,
                tasks = listOf(
                    ServiceTask("t9",  "Oil change",       isDone = true, completedBy = "Simon S.", completedAt = "08:10"),
                    ServiceTask("t10", "Brake check",      isDone = true, completedBy = "Simon S.", completedAt = "08:30"),
                    ServiceTask("t11", "Air filter swap",  isDone = true, completedBy = "Simon S.", completedAt = "08:50"),
                )
            ),
        )
    )

    // Mark a task as done — only if it hasn't been done yet (enforces single mechanic rule)
    fun markTaskDone(jobId: String, taskId: String, mechanicName: String, note: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        jobs = jobs.map { job ->
            if (job.id != jobId) return@map job
            val updatedTasks = job.tasks.map { task ->
                if (task.id == taskId && !task.isDone) {
                    task.copy(isDone = true, completedBy = mechanicName, completedAt = time, note = note)
                } else task
            }
            // Update truck status based on tasks
            val allDone = updatedTasks.all { it.isDone }
            val anyDone = updatedTasks.any { it.isDone }
            job.copy(
                tasks = updatedTasks,
                status = when {
                    allDone -> TruckStatus.DONE
                    anyDone -> TruckStatus.IN_PROGRESS
                    else    -> TruckStatus.WAITING
                }
            )
        }
    }

    // Add a custom task to a truck's job
    fun addCustomTask(jobId: String, taskName: String) {
        if (taskName.isBlank()) return
        jobs = jobs.map { job ->
            if (job.id != jobId) return@map job
            val newTask = ServiceTask(
                id = "custom_${System.currentTimeMillis()}",
                name = taskName
            )
            job.copy(tasks = job.tasks + newTask)
        }
    }

    fun getJob(jobId: String): TruckJob? = jobs.find { it.id == jobId }
}

// ─── Screen 1: Vehicle Picker ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicVehiclePickerScreen(
    navController: NavController,
    viewModel: MechanicViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Task Board")
                        Text(
                            "Select a vehicle to service",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "VEHICLES ON FLOOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(viewModel.jobs, key = { it.id }) { job ->
                VehiclePickerCard(
                    job = job,
                    onClick = {
                        navController.navigate("taskBoard/${job.id}")
                    }
                )
            }
        }
    }
}

@Composable
fun VehiclePickerCard(
    job: TruckJob,
    onClick: () -> Unit
) {
    val doneTasks  = job.tasks.count { it.isDone }
    val totalTasks = job.tasks.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = job.licencePlate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                    StatusBadge(job.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${job.driverName}  ·  $doneTasks / $totalTasks tasks done",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open task board",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}



// ─── Screen 2: Task Board ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicTaskBoardScreen(
    navController: NavController,
    jobId: String,
    // In a real app pass the logged-in mechanic's name from your auth state
    currentMechanic: String = "David M.",

    viewModel: MechanicViewModel = viewModel()
) {
    val job = viewModel.getJob(jobId)

    if (job == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vehicle not found")
        }
        return
    }

    // Per-task note input state — stored locally until the mechanic submits
    val noteState = remember { mutableStateMapOf<String, String>() }

    // Custom task dialog state
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var customTaskInput   by remember { mutableStateOf("") }

    val doneTasks  = job.tasks.count { it.isDone }
    val totalTasks = job.tasks.size
    val progress   = if (totalTasks > 0) doneTasks.toFloat() / totalTasks else 0f

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false; customTaskInput = "" },
            title = { Text("Add custom task") },
            text = {
                OutlinedTextField(
                    value = customTaskInput,
                    onValueChange = { customTaskInput = it },
                    placeholder = { Text("e.g. Replace wiper blades") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addCustomTask(job.id, customTaskInput)
                    customTaskInput = ""
                    showAddTaskDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false; customTaskInput = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = job.licencePlate,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${job.driverName}  ·  ${job.status.label}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF1A1A1A)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task", tint = Color.White)
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {

            // ── Progress header ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SERVICE TASKS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "$doneTasks / $totalTasks done",
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
                Spacer(Modifier.height(12.dp))
            }

            // ── Task items ────────────────────────────────────────────────
            items(job.tasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    currentMechanic = currentMechanic,
                    noteValue = noteState[task.id] ?: "",
                    onNoteChange = { noteState[task.id] = it },
                    onMarkDone = {
                        viewModel.markTaskDone(
                            jobId = job.id,
                            taskId = task.id,
                            mechanicName = currentMechanic,
                            note = noteState[task.id] ?: ""
                        )
                        noteState.remove(task.id)
                    }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

// ─── Task item ────────────────────────────────────────────────────────────────

@Composable
fun TaskItem(
    task: ServiceTask,
    currentMechanic: String,
    noteValue: String,
    onNoteChange: (String) -> Unit,
    onMarkDone: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Checkbox — tappable only if not yet done
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { if (!task.isDone) onMarkDone() },
                enabled = !task.isDone,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF1A1A1A),
                    uncheckedColor = Color.LightGray
                ),
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {

                // Task name — strikethrough when done
                Text(
                    text = task.name,
                    fontSize = 14.sp,
                    color = if (task.isDone)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    style = if (task.isDone)
                        LocalTextStyle.current.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    else LocalTextStyle.current
                )

                Spacer(Modifier.height(3.dp))

                if (task.isDone) {
                    // Show who did it and when
                    Text(
                        text = "${task.completedBy}  ·  ${task.completedAt}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.note.isNotBlank()) {
                        Text(
                            text = task.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                } else {
                    // Note input — only show for the current mechanic on undone tasks
                    Text(
                        text = "Unclaimed",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = noteValue,
                        onValueChange = onNoteChange,
                        placeholder = { Text("Add a note before marking done…", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            // Mark done button inside the text field
                            TextButton(
                                onClick = onMarkDone,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Done", fontSize = 11.sp, color = Color(0xFF1A1A1A))
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─── Status badge (shared) ────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: TruckStatus) {
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

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun VehiclePickerPreview() {
    MechanicVehiclePickerScreen(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun TaskBoardPreview() {
    MechanicTaskBoardScreen(navController = rememberNavController(), jobId = "1")
}