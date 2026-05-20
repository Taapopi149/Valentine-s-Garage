package com.example.valentinesgarage.Screens.Manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.valentinesgarage.Data.DAO.NotesDao
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Data.Entities.Truck
import com.example.valentinesgarage.Screens.CheckIn.VehicleCondition
import com.example.valentinesgarage.Screens.Employee.ViewModelFactory.ReportsViewModelFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── UI models ────────────────────────────────────────────────────────────────

data class EmployeeActivity(
    val employeeId:     String,
    val name:           String,
    val role:           String,
    val totalTasks:     Int,
    val tasks:          List<TaskEntry>,
    val note:           String,
    val avatarColor:    Color,
    val avatarTextColor: Color
)

data class TaskEntry(val label: String, val truckCount: Int)

data class VehicleCheckIn(
    val truckId:   String,
    val date:      String,
    val odometer:  String,
    val condition: VehicleCondition,
    val notes:     String
)

// ─── Avatar colour pool — cycles through for each mechanic ───────────────────

private val avatarPalette = listOf(
    Color(0xFFE6F1FB) to Color(0xFF0C447C),
    Color(0xFFE1F5EE) to Color(0xFF085041),
    Color(0xFFFAEEDA) to Color(0xFF633806),
    Color(0xFFEEEDFE) to Color(0xFF3C3489),
    Color(0xFFFCEBEB) to Color(0xFF791F1F),
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ReportsViewModel(
    private val tasksDao: TasksDao,
    private val truckDao: TruckDao,
    private val userDao:  UserDao,
    private val notesDao: NotesDao
) : ViewModel() {

    // ── Metrics ───────────────────────────────────────────────────────────────

    val truckCount: StateFlow<Int> =
        truckDao.getTruckCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedTaskCount: StateFlow<Int> =
        tasksDao.getCompletedTaskCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingTaskCount: StateFlow<Int> =
        tasksDao.getPendingTaskCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val mechanicCount: StateFlow<Int> =
        userDao.getAllMechanics()
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Employee activity cards ───────────────────────────────────────────────

    private val _employeeActivities = MutableStateFlow<List<EmployeeActivity>>(emptyList())
    val employeeActivities: StateFlow<List<EmployeeActivity>> = _employeeActivities.asStateFlow()

    // ── Vehicle check-in log ──────────────────────────────────────────────────

    private val _checkIns = MutableStateFlow<List<VehicleCheckIn>>(emptyList())
    val checkIns: StateFlow<List<VehicleCheckIn>> = _checkIns.asStateFlow()

    // ── Mechanic names for filter ─────────────────────────────────────────────

    val mechanicNames: StateFlow<List<String>> =
        userDao.getAllMechanics()
            .map { list -> list.map { "${it.firstName} ${it.lastName}" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadEmployeeActivities()
        loadCheckIns()
    }

    private fun loadEmployeeActivities() {
        viewModelScope.launch {
            userDao.getAllMechanics().collect { mechanics ->
                _employeeActivities.value = mechanics.mapIndexed { index, user ->
                    val taskSummary  = tasksDao.getTaskSummaryForEmployee(user.employeeId)
                    val totalTasks   = tasksDao.getTotalTasksForEmployee(user.employeeId)
                    val latestNote   = tasksDao.getLatestNoteForEmployee(user.employeeId) ?: "No notes yet."
                    val (bg, fg)     = avatarPalette[index % avatarPalette.size]

                    EmployeeActivity(
                        employeeId      = user.employeeId,
                        name            = "${user.firstName} ${user.lastName}",
                        role            = user.role,
                        totalTasks      = totalTasks,
                        tasks           = taskSummary.map { TaskEntry(it.taskLabel, it.truckCount) },
                        note            = latestNote,
                        avatarColor     = bg,
                        avatarTextColor = fg
                    )
                }
            }
        }
    }

    private fun loadCheckIns() {
        viewModelScope.launch {
            truckDao.getAllTruck().collect { trucks ->
                _checkIns.value = trucks.map { truck ->
                    val note = notesDao.getNoteForTruck(truck.truckId) ?: ""
                    truck.toVehicleCheckIn(note)
                }
            }
        }
    }
}

// ─── Mapper ───────────────────────────────────────────────────────────────────

private fun Truck.toVehicleCheckIn(note: String) = VehicleCheckIn(
    truckId   = licencePlate,
    date      = checkInTime,
    odometer  = "$Odmeter km",
    condition = Condition,
    notes     = note
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    tasksDao:      TasksDao,
    truckDao:      TruckDao,
    userDao:       UserDao,
    notesDao:      NotesDao
) {
    val viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(tasksDao, truckDao, userDao, notesDao)
    )

    var selectedMechanic by remember { mutableStateOf("All mechanics") }

    val truckCount          by viewModel.truckCount.collectAsState()
    val completedTaskCount  by viewModel.completedTaskCount.collectAsState()
    val pendingTaskCount    by viewModel.pendingTaskCount.collectAsState()
    val mechanicCount       by viewModel.mechanicCount.collectAsState()
    val employeeActivities  by viewModel.employeeActivities.collectAsState()
    val checkIns            by viewModel.checkIns.collectAsState()
    val mechanicNames       by viewModel.mechanicNames.collectAsState()

    // Filter employee cards by selected mechanic
    val filteredActivities = remember(selectedMechanic, employeeActivities) {
        if (selectedMechanic == "All mechanics") employeeActivities
        else employeeActivities.filter { it.name == selectedMechanic }
    }

    Scaffold(
        topBar         = { ReportsTopBar() },
        containerColor = Color(0xFFF8F8F6)
    ) { paddingValues ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Filter bar — mechanic list from DB
            item {
                FilterBar(
                    selectedMechanic = selectedMechanic,
                    mechanicNames    = mechanicNames,
                    onMechanicChange = { selectedMechanic = it }
                )
            }

            // Metrics — live from DB
            item {
                MetricsRow(
                    truckCount         = truckCount,
                    completedTaskCount = completedTaskCount,
                    pendingTaskCount   = pendingTaskCount,
                    mechanicCount      = mechanicCount
                )
            }

            // Employee activity
            item {
                SectionHeaderReport(
                    title = "Employee activity",
                    icon  = Icons.Outlined.Person
                )
            }
            item {
                if (filteredActivities.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No activity yet", color = Color(0xFF888780), fontSize = 13.sp)
                    }
                } else {
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        items(filteredActivities, key = { it.employeeId }) { emp ->
                            EmployeeCard(emp)
                        }
                    }
                }
            }

            // Vehicle check-in log
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeaderReport(
                    title = "Vehicle check-in log",
                    icon  = Icons.Outlined.DirectionsCar
                )
            }

            if (checkIns.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No check-ins yet", color = Color(0xFF888780), fontSize = 13.sp)
                    }
                }
            } else {
                items(checkIns, key = { it.truckId }) { checkIn ->
                    CheckInRow(checkIn)
                }
            }
        }
    }
}

// ─── Top bar (unchanged) ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTopBar() {
    TopAppBar(
        title = {
            Column {
                Text("Reports", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
                Text("Valentine's Garage", fontSize = 12.sp, color = Color(0xFF888780))
            }
        },
        actions = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = "Export", tint = Color(0xFF534AB7))
                }
                Box(
                    modifier         = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFEEEDFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VL", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF3C3489))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

// ─── Filter bar — now takes mechanic list from DB ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    selectedMechanic: String,
    mechanicNames:    List<String>,
    onMechanicChange: (String) -> Unit
) {
    val allOptions = listOf("All mechanics") + mechanicNames
    var expanded   by remember { mutableStateOf(false) }

    Surface(color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            FilterChip(
                selected    = false,
                onClick     = {},
                label       = { Text("Apr 2025", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                shape  = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFFF1EFE8),
                    labelColor     = Color(0xFF2C2C2A)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled     = true,
                    selected    = false,
                    borderColor = Color(0xFFD3D1C7),
                    borderWidth = 0.5.dp
                )
            )

            Box {
                FilterChip(
                    selected      = selectedMechanic != "All mechanics",
                    onClick       = { expanded = true },
                    label         = { Text(selectedMechanic, fontSize = 12.sp, maxLines = 1) },
                    trailingIcon  = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    shape  = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor          = Color(0xFFF1EFE8),
                        selectedContainerColor  = Color(0xFFEEEDFE),
                        labelColor              = Color(0xFF2C2C2A),
                        selectedLabelColor      = Color(0xFF3C3489)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = selectedMechanic != "All mechanics",
                        borderColor         = Color(0xFFD3D1C7),
                        selectedBorderColor = Color(0xFFAFA9EC),
                        borderWidth         = 0.5.dp
                    )
                )
                DropdownMenu(
                    expanded         = expanded,
                    onDismissRequest = { expanded = false },
                    modifier         = Modifier.background(Color.White)
                ) {
                    allOptions.forEach { name ->
                        DropdownMenuItem(
                            text    = { Text(name, fontSize = 13.sp) },
                            onClick = { onMechanicChange(name); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

// ─── Metrics row — now takes live values ─────────────────────────────────────

@Composable
fun MetricsRow(
    truckCount:         Int,
    completedTaskCount: Int,
    pendingTaskCount:   Int,
    mechanicCount:      Int
) {
    val metrics = listOf(
        Triple("Checked in", truckCount.toString(),         Color(0xFF1A1A1A)),
        Triple("Completed",  completedTaskCount.toString(), Color(0xFF1A1A1A)),
        Triple("Pending",    pendingTaskCount.toString(),   Color(0xFFBA7517)),
        Triple("Mechanics",  mechanicCount.toString(),      Color(0xFF1A1A1A))
    )
    Row(
        modifier              = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.forEach { (label, value, valueColor) ->
            MetricCard(label = label, value = value, valueColor = valueColor, modifier = Modifier.weight(1f))
        }
    }
}

// ─── Metric card (unchanged) ──────────────────────────────────────────────────

@Composable
fun MetricCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0DED5))
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = Color(0xFF888780), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

// ─── Section header (unchanged) ───────────────────────────────────────────────

@Composable
fun SectionHeaderReport(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier              = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF888780))
        Text(
            title.uppercase(),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Medium,
            color         = Color(0xFF888780),
            letterSpacing = 0.8.sp
        )
    }
}

// ─── Employee card (unchanged layout, data now from DB) ───────────────────────

@Composable
fun EmployeeCard(employee: EmployeeActivity) {
    Card(
        modifier  = Modifier.width(260.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0DED5))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(38.dp).clip(CircleShape)
                        .background(employee.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    // Use real initials from DB name
                    Text(
                        text       = employee.name
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2)
                            .joinToString(""),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = employee.avatarTextColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(employee.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                    Text(employee.role, fontSize = 11.sp, color = Color(0xFF888780))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEAF3DE))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${employee.totalTasks} tasks",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color(0xFF27500A)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0EDE6), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            if (employee.tasks.isEmpty()) {
                Text("No tasks yet", fontSize = 12.sp, color = Color(0xFF888780))
            } else {
                employee.tasks.forEach { task ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(6.dp).clip(CircleShape)
                                    .background(Color(0xFFD3D1C7))
                            )
                            Text(task.label, fontSize = 12.sp, color = Color(0xFF444441))
                        }
                        Text(
                            "${task.truckCount} truck${if (task.truckCount > 1) "s" else ""}",
                            fontSize = 11.sp,
                            color    = Color(0xFF888780)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0EDE6), thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Notes,
                    contentDescription = null,
                    modifier           = Modifier.size(13.dp).padding(top = 2.dp),
                    tint               = Color(0xFFB4B2A9)
                )
                Text(
                    "\"${employee.note}\"",
                    fontSize   = 11.sp,
                    color      = Color(0xFF888780),
                    fontStyle  = FontStyle.Italic,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─── Check-in row (unchanged layout, data now from DB) ────────────────────────

@Composable
fun CheckInRow(checkIn: VehicleCheckIn) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0DED5))
        )
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(
                modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1EFE8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp),
                    tint               = Color(0xFF5F5E5A)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(checkIn.truckId, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                    ConditionBadge(checkIn.condition)
                }
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null,
                            modifier = Modifier.size(11.dp), tint = Color(0xFFB4B2A9))
                        Text(checkIn.date, fontSize = 11.sp, color = Color(0xFF888780))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Speed, contentDescription = null,
                            modifier = Modifier.size(11.dp), tint = Color(0xFFB4B2A9))
                        Text(checkIn.odometer, fontSize = 11.sp, color = Color(0xFF888780))
                    }
                }
                if (checkIn.notes.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        checkIn.notes,
                        fontSize  = 12.sp,
                        color     = Color(0xFF5F5E5A),
                        lineHeight = 16.sp,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Condition badge (unchanged) ──────────────────────────────────────────────

@Composable
fun ConditionBadge(condition: VehicleCondition) {
    val (bg, text, label) = when (condition) {
        VehicleCondition.EXCELLENT -> Triple(Color(0xFFEAF3DE), Color(0xFF27500A), "Excellent")
        VehicleCondition.GOOD      -> Triple(Color(0xFFEAF3DE), Color(0xFF27500A), "Good")
        VehicleCondition.FAIR      -> Triple(Color(0xFFFAEEDA), Color(0xFF633806), "Fair")
        VehicleCondition.POOR      -> Triple(Color(0xFFFCEBEB), Color(0xFF791F1F), "Poor")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = text)
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFFF8F8F6)
@Composable
fun ReportsScreenPreview() {
    MaterialTheme {
        Scaffold(
            topBar         = { ReportsTopBar() },
            containerColor = Color(0xFFF8F8F6)
        ) { padding ->
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    MetricsRow(
                        truckCount         = 14,
                        completedTaskCount = 38,
                        pendingTaskCount   = 5,
                        mechanicCount      = 4
                    )
                }
                item {
                    SectionHeaderReport("Employee activity", Icons.Outlined.Person)
                }
                item {
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listOf(
                            EmployeeActivity(
                                employeeId      = "1",
                                name            = "James Mokoena",
                                role            = "Mechanic",
                                totalTasks      = 8,
                                tasks           = listOf(TaskEntry("Oil change", 3), TaskEntry("Brake check", 2)),
                                note            = "Noticed worn pads on T-447.",
                                avatarColor     = Color(0xFFE6F1FB),
                                avatarTextColor = Color(0xFF0C447C)
                            )
                        )) { emp -> EmployeeCard(emp) }
                    }
                }
            }
        }
    }
}