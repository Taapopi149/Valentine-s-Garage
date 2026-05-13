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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.Entities.Tasks
import com.example.valentinesgarage.Data.Entities.Truck
import com.example.valentinesgarage.Screens.Employee.ViewModelFactory.MechanicViewModelFactory
import com.example.valentinesgarage.Screens.Vehicles.TruckStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ─── UI models ────────────────────────────────────────────────────────────────

data class ServiceTask(
    val id: Int,
    val name: String,
    val isDone: Boolean = false,
    val completedBy: String = "",
    val completedAt: String = "",
    val note: String = ""
)

data class TruckJob(
    val id: Int,
    val licencePlate: String,
    val driverName: String,
    val status: TruckStatus,
    val tasks: List<ServiceTask>
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class MechanicViewModel(
    private val truckDao: TruckDao,
    private val tasksDao: TasksDao
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<TruckJob>>(emptyList())
    val jobs: StateFlow<List<TruckJob>> = _jobs.asStateFlow()

    init {
        loadJobs()
    }

    private fun loadJobs() {
        viewModelScope.launch {
            // Collect all trucks, then for each truck collect its tasks
            truckDao.getAllTruck().collect { trucks ->
                // Build a combined flow for all trucks + their tasks
                val jobList = trucks.map { truck ->
                    val tasks = tasksDao.getTasksForTruckOnce(truck.truckId)
                    truck.toTruckJob(tasks)
                }
                _jobs.value = jobList
            }
        }
    }

    fun getJob(jobId: Int): TruckJob? = _jobs.value.find { it.id == jobId }

    // ── Mark task done ────────────────────────────────────────────────────────

    fun markTaskDone(
        jobId: Int,
        taskId: Int,
        mechanicName: String,
        note: String
    ) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        viewModelScope.launch {
            // 1. Write to DB
            tasksDao.markTaskComplete(
                taskId      = taskId,
                status      = "Done",
                completedBy = mechanicName,
                completedAt = time,
                note        = note
            )

            // 2. Update local state immediately so UI reacts without waiting for DB flow
            _jobs.value = _jobs.value.map { job ->
                if (job.id != jobId) return@map job
                val updatedTasks = job.tasks.map { task ->
                    if (task.id == taskId && !task.isDone)
                        task.copy(isDone = true, completedBy = mechanicName, completedAt = time, note = note)
                    else task
                }
                val allDone = updatedTasks.all { it.isDone }
                val anyDone = updatedTasks.any { it.isDone }
                val newStatus = when {
                    allDone -> TruckStatus.DONE
                    anyDone -> TruckStatus.IN_PROGRESS
                    else    -> TruckStatus.WAITING
                }
                // Also persist the new truck status
                truckDao.updateTruckStatus(jobId, newStatus)
                job.copy(tasks = updatedTasks, status = newStatus)
            }
        }
    }

    // ── Add custom task ───────────────────────────────────────────────────────

    fun addCustomTask(jobId: Int, taskName: String, employeeId: String = "") {
        if (taskName.isBlank()) return
        viewModelScope.launch {
            tasksDao.insertTask(
                Tasks(
                    description  = taskName,
                    employeeIdFk = employeeId,
                    truckIdOwner = jobId,
                    status       = "Pending",
                    priority     = "Medium"
                )
            )
            // Refresh tasks for this truck
            val updatedTasks = tasksDao.getTasksForTruckOnce(jobId)
            _jobs.value = _jobs.value.map { job ->
                if (job.id != jobId) job
                else job.copy(tasks = updatedTasks.map { it.toServiceTask() })
            }
        }
    }
}

// ─── Mappers ──────────────────────────────────────────────────────────────────

private fun Tasks.toServiceTask() = ServiceTask(
    id          = Taskid,
    name        = description,
    isDone      = status == "Done",
    completedBy = completedBy,
    completedAt = completedAt,
    note        = note
)

private fun Truck.toTruckJob(tasks: List<Tasks>): TruckJob {
    val serviceTasks = tasks.map { it.toServiceTask() }
    return TruckJob(
        id           = truckId,
        licencePlate = licencePlate,
        driverName   = DriverName,
        status       = truckStatus,
        tasks        = serviceTasks
    )
}

// ─── Extra DAO helpers needed ─────────────────────────────────────────────────
// Add these to TasksDao and TruckDao:
//
// TasksDao:
//   @Query("SELECT * FROM Tasks WHERE truckIdOwner = :truckId")
//   suspend fun getTasksForTruckOnce(truckId: Int): List<Tasks>
//
// TruckDao:
//   @Query("UPDATE Truck SET truckStatus = :status WHERE truckId = :truckId")
//   suspend fun updateTruckStatus(truckId: Int, status: TruckStatus)

// ─── Screen 1: Vehicle Picker ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicVehiclePickerScreen(
    navController: NavController,
    truckDao: TruckDao,
    tasksDao: TasksDao
) {
    val viewModel: MechanicViewModel = viewModel(
        factory = MechanicViewModelFactory(truckDao, tasksDao)
    )

    val jobs by viewModel.jobs.collectAsState()

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

        if (jobs.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No vehicles on the floor", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier        = Modifier.padding(padding),
                contentPadding  = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text          = "VEHICLES ON FLOOR",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                        modifier      = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(jobs, key = { it.id }) { job ->
                    VehiclePickerCard(
                        job     = job,
                        onClick = { navController.navigate("taskBoard/${job.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun VehiclePickerCard(job: TruckJob, onClick: () -> Unit) {
    val doneTasks  = job.tasks.count { it.isDone }
    val totalTasks = job.tasks.size

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = job.licencePlate,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                    StatusBadge(job.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${job.driverName}  ·  $doneTasks / $totalTasks tasks done",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = "Open task board",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Screen 2: Task Board ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicTaskBoardScreen(
    navController: NavController,
    jobId: Int,
    currentMechanic: String = "David M.",
    truckDao: TruckDao,
    tasksDao: TasksDao
) {
    val viewModel: MechanicViewModel = viewModel(
        factory = MechanicViewModelFactory(truckDao, tasksDao)
    )

    val jobs by viewModel.jobs.collectAsState()
    val job   = jobs.find { it.id == jobId }

    if (job == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vehicle not found")
        }
        return
    }

    val noteState = remember { mutableStateMapOf<Int, String>() }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var customTaskInput   by remember { mutableStateOf("") }

    val doneTasks  = job.tasks.count { it.isDone }
    val totalTasks = job.tasks.size
    val progress   = if (totalTasks > 0) doneTasks.toFloat() / totalTasks else 0f

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false; customTaskInput = "" },
            title            = { Text("Add custom task") },
            text = {
                OutlinedTextField(
                    value         = customTaskInput,
                    onValueChange = { customTaskInput = it },
                    placeholder   = { Text("e.g. Replace wiper blades") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addCustomTask(job.id, customTaskInput)
                    customTaskInput   = ""
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
                            text       = job.licencePlate,
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text  = "${job.driverName}  ·  ${job.status.label}",
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
                onClick        = { showAddTaskDialog = true },
                containerColor = Color(0xFF1A1A1A)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task", tint = Color.White)
            }
        }
    ) { padding ->

        LazyColumn(
            modifier        = Modifier.padding(padding),
            contentPadding  = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {

            // ── Progress header ───────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "SERVICE TASKS",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "$doneTasks / $totalTasks done",
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
                Spacer(Modifier.height(12.dp))
            }

            // ── Task items ────────────────────────────────────────────────
            items(job.tasks, key = { it.id }) { task ->
                TaskItem(
                    task            = task,
                    currentMechanic = currentMechanic,
                    noteValue       = noteState[task.id] ?: "",
                    onNoteChange    = { noteState[task.id] = it },
                    onMarkDone      = {
                        viewModel.markTaskDone(
                            jobId         = job.id,
                            taskId        = task.id,
                            mechanicName  = currentMechanic,
                            note          = noteState[task.id] ?: ""
                        )
                        noteState.remove(task.id)
                    }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant
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
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked         = task.isDone,
                onCheckedChange = { if (!task.isDone) onMarkDone() },
                enabled         = !task.isDone,
                colors          = CheckboxDefaults.colors(
                    checkedColor   = Color(0xFF1A1A1A),
                    uncheckedColor = Color.LightGray
                ),
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = task.name,
                    fontSize = 14.sp,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    style = if (task.isDone)
                        LocalTextStyle.current.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    else LocalTextStyle.current
                )
                Spacer(Modifier.height(3.dp))

                if (task.isDone) {
                    Text(
                        text     = "${task.completedBy}  ·  ${task.completedAt}",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.note.isNotBlank()) {
                        Text(
                            text      = task.note,
                            fontSize  = 11.sp,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                } else {
                    Text(
                        text     = "Unclaimed",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value         = noteValue,
                        onValueChange = onNoteChange,
                        placeholder   = { Text("Add a note before marking done…", fontSize = 11.sp) },
                        singleLine    = true,
                        textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(8.dp),
                        trailingIcon  = {
                            TextButton(
                                onClick        = onMarkDone,
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

// ─── Status badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: TruckStatus) {
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

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun VehiclePickerPreview() {
    MaterialTheme {
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(
                TruckJob(1, "N 12345 W", "Johannes Shikongo", TruckStatus.IN_PROGRESS, listOf(
                    ServiceTask(1, "Oil change", isDone = true, completedBy = "David M.", completedAt = "09:12"),
                    ServiceTask(2, "Brake check")
                )),
                TruckJob(2, "N 78900 W", "Petrus Hamutenya", TruckStatus.WAITING, listOf(
                    ServiceTask(3, "Full inspection")
                ))
            )) { job -> VehiclePickerCard(job = job, onClick = {}) }
        }
    }
}