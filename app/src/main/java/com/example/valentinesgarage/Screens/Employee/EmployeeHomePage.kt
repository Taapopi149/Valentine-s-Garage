package com.example.valentinesgarage.Screens.Employee


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DataClasses.TaskWithPlate
import com.example.valentinesgarage.R
import com.example.valentinesgarage.Screens.Employee.ViewModelFactory.EmployeeHomeViewModelFactory
import com.example.valentinesgarage.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ─── Data models ──────────────────────────────────────────────────────────────

data class RecentTask(
    val id: String,
    val title: String,
    val truckPlate: String,
    val priority: TaskPriority,
    val status: TaskStatus
)

data class TruckWithTasks(
    val plate: String,
    val model: String,
    val taskCount: Int,
    val currentTask: String?,
    val status: TruckStatus
)

enum class TaskPriority { HIGH, MEDIUM, LOW }
enum class TaskStatus   { PENDING, IN_PROGRESS, DONE }
enum class TruckStatus  { AVAILABLE, IN_SERVICE, WAITING }

// ─── ViewModel ────────────────────────────────────────────────────────────────

class EmployeeHomeViewModel(
    private val tasksDao: TasksDao,
    private val truckDao: TruckDao,
    private val employeeId: String
) : ViewModel() {

    val recentTasks: StateFlow<List<RecentTask>> =
        tasksDao.getRecentTasksForEmployee(employeeId)
            .map { list -> list.map { it.toRecentTask() } }
            .stateIn(
                scope            = viewModelScope,
                started          = SharingStarted.WhileSubscribed(5000),
                initialValue     = emptyList()
            )

    private val _trucksWithTasks = MutableStateFlow<List<TruckWithTasks>>(emptyList())
    val trucksWithTasks: StateFlow<List<TruckWithTasks>> = _trucksWithTasks.asStateFlow()

    init {
        loadTrucksWithTasks()
    }

    private fun loadTrucksWithTasks() {
        viewModelScope.launch {
            truckDao.getAllTruck().collect { trucks ->
                _trucksWithTasks.value = trucks.map { truck ->
                    TruckWithTasks(
                        plate       = truck.licencePlate,
                        model       = "",
                        taskCount   = truckDao.getTaskCountForTruck(truck.truckId),
                        currentTask = truckDao.getCurrentTaskForTruck(truck.truckId),
                        status      = truck.truckStatus.toUiStatus()
                    )
                }
            }
        }
    }
}

// ─── Mappers ──────────────────────────────────────────────────────────────────

private fun TaskWithPlate.toRecentTask() = RecentTask(
    id         = taskId.toString(),
    title      = description,
    truckPlate = licencePlate,
    priority   = when (priority.lowercase()) {
        "high" -> TaskPriority.HIGH
        "low"  -> TaskPriority.LOW
        else   -> TaskPriority.MEDIUM
    },
    status     = when (status) {
        "In Progress" -> TaskStatus.IN_PROGRESS
        "Done"        -> TaskStatus.DONE
        else          -> TaskStatus.PENDING
    }
)

private fun com.example.valentinesgarage.Screens.Vehicles.TruckStatus.toUiStatus() =
    when (this) {
        com.example.valentinesgarage.Screens.Vehicles.TruckStatus.WAITING     -> TruckStatus.WAITING
        com.example.valentinesgarage.Screens.Vehicles.TruckStatus.IN_PROGRESS -> TruckStatus.IN_SERVICE
        com.example.valentinesgarage.Screens.Vehicles.TruckStatus.DONE        -> TruckStatus.AVAILABLE
    }

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeHomePage(
    navController: NavController,
    truckDao: TruckDao,
    tasksDao: TasksDao
) {
    val employeeId = SessionManager.currentUserId ?: ""

    val viewModel: EmployeeHomeViewModel = viewModel(
        factory = EmployeeHomeViewModelFactory(tasksDao, truckDao, employeeId)
    )

    val recentTasks     by viewModel.recentTasks.collectAsState()
    val trucksWithTasks by viewModel.trucksWithTasks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("Login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { navController.navigate("Profile") }) {
                        Icon(
                            imageVector        = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding      = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {

            // ── Top cards ────────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CardDesign(
                        imageRs  = R.drawable.activetruckimg,
                        title    = "Trucks in Garage",
                        modifier = Modifier.weight(1f),
                        onClick  = { navController.navigate("ActiveVehicle") }
                    )
                    CardDesign(
                        imageRs  = R.drawable.taskspic,
                        title    = "Tasks",
                        modifier = Modifier.weight(1f),
                        onClick  = { navController.navigate("MechanicTaskList") }
                    )
                }
            }

            // ── Check-in button ───────────────────────────────────────────────
            item {
                Button(
                    onClick  = { navController.navigate("TruckCheckIn") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check In Truck", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            // ── Recent Tasks — from DB ────────────────────────────────────────
            item {
                SectionHeader(
                    title    = "Recent Tasks",
                    onSeeAll = { navController.navigate("MechanicTaskList") }
                )
            }

            if (recentTasks.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tasks assigned yet", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    }
                }
            } else {
                items(recentTasks) { task ->
                    RecentTaskCard(task = task)
                }
            }

            // ── Trucks & Tasks — from DB ──────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(
                    title    = "Trucks & Their Tasks",
                    onSeeAll = { navController.navigate("ActiveVehicle") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (trucksWithTasks.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No trucks checked in", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding        = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(trucksWithTasks) { truck ->
                            TruckTaskCard(truck = truck)
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text       = title,
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF1A1A1A)
        )
        TextButton(onClick = onSeeAll) {
            Text(text = "See all", fontSize = 13.sp, color = Color(0xFF555555))
        }
    }
}

// ─── Recent Task Card ─────────────────────────────────────────────────────────

@Composable
fun RecentTaskCard(task: RecentTask) {
    val (statusColor, statusLabel, statusIcon) = when (task.status) {
        TaskStatus.IN_PROGRESS -> Triple(Color(0xFF1565C0), "In Progress", Icons.Default.Build)
        TaskStatus.PENDING     -> Triple(Color(0xFFE65100), "Pending",     Icons.Default.Schedule)
        TaskStatus.DONE        -> Triple(Color(0xFF2E7D32), "Done",        Icons.Default.CheckCircle)
    }

    val priorityColor = when (task.priority) {
        TaskPriority.HIGH   -> Color(0xFFD32F2F)
        TaskPriority.MEDIUM -> Color(0xFFF57C00)
        TaskPriority.LOW    -> Color(0xFF388E3C)
    }

    Card(
        shape    = RoundedCornerShape(12.dp),
        elevation = cardElevation(2.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier.size(44.dp).clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color      = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint     = Color(0xFF888888),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(text = task.truckPlate, fontSize = 12.sp, color = Color(0xFF888888))
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        text       = statusLabel,
                        color      = statusColor,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(7.dp).clip(CircleShape).background(priorityColor)
                    )
                    Text(
                        text       = task.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize   = 11.sp,
                        color      = priorityColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Truck Task Card ──────────────────────────────────────────────────────────

@Composable
fun TruckTaskCard(truck: TruckWithTasks) {
    val (statusColor, statusLabel) = when (truck.status) {
        TruckStatus.IN_SERVICE -> Pair(Color(0xFF1565C0), "In Service")
        TruckStatus.WAITING    -> Pair(Color(0xFFE65100), "Waiting")
        TruckStatus.AVAILABLE  -> Pair(Color(0xFF2E7D32), "Available")
    }

    Card(
        shape    = RoundedCornerShape(14.dp),
        elevation = cardElevation(3.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.width(175.dp).wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint     = Color(0xFF444444),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.13f)) {
                    Text(
                        text       = statusLabel,
                        color      = statusColor,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = truck.plate, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A1A1A))
            Text(text = truck.model, fontSize = 12.sp, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))

            if (truck.currentTask != null) {
                Text(text = "Current Task", fontSize = 10.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text       = truck.currentTask,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF333333),
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
            } else {
                Text(text = "No active tasks", fontSize = 12.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                Text(
                    text     = "${truck.taskCount} task${if (truck.taskCount != 1) "s" else ""} total",
                    fontSize = 11.sp,
                    color    = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─── Card Design ──────────────────────────────────────────────────────────────

@Composable
fun CardDesign(
    imageRs: Int,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(16.dp),
        elevation = cardElevation(8.dp),
        modifier  = modifier.fillMaxHeight()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter            = painterResource(id = imageRs),
                contentDescription = title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            Box(
                modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomepagePreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(title = { Text("Employee Dashboard") })
            }
        ) { padding ->
            LazyColumn(
                modifier       = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
            ) {
                items(listOf(
                    RecentTask("1", "Oil Change", "NAM 1234", TaskPriority.HIGH,   TaskStatus.IN_PROGRESS),
                    RecentTask("2", "Brake Check", "WDH 5678", TaskPriority.LOW,   TaskStatus.PENDING),
                    RecentTask("3", "Tyre Rotation", "NAM 9012", TaskPriority.MEDIUM, TaskStatus.DONE),
                )) { task -> RecentTaskCard(task = task) }
            }
        }
    }
}