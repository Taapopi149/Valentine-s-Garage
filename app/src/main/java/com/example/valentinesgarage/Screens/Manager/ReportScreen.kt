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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.valentinesgarage.Screens.CheckIn.VehicleCondition

// ── Data models ──────────────────────────────────────────────────────────────

data class EmployeeActivity(

    val name: String,
    val role: String,
    val totalTasks: Int,
    val tasks: List<TaskEntry>,
    val note: String,
    val avatarColor: Color,
    val avatarTextColor: Color
)

data class TaskEntry(val label: String, val truckCount: Int)

data class VehicleCheckIn(
    val truckId: String,
    val date: String,
    val odometer: String,
    val condition: VehicleCondition,
    val notes: String
)



// ── Sample data ───────────────────────────────────────────────────────────────

val sampleEmployees = listOf(
    EmployeeActivity(
        name = "J. Kambonde", role = "Mechanic", totalTasks = 8,
        tasks = listOf(
            TaskEntry("Engine oil change", 3),
            TaskEntry("Brake inspection", 2),
            TaskEntry("Tyre rotation", 2),
            TaskEntry("Coolant flush", 1)
        ),
        note = "Noticed worn pads on T-447, replaced and flagged for follow-up.",
        avatarColor = Color(0xFFE6F1FB), avatarTextColor = Color(0xFF0C447C)
    ),
    EmployeeActivity(
        name = "P. Nangolo", role = "Mechanic", totalTasks = 7,
        tasks = listOf(
            TaskEntry("Transmission check", 3),
            TaskEntry("Air filter replacement", 2),
            TaskEntry("Suspension inspection", 2)
        ),
        note = "T-112 has a small oil leak near the gasket. Owner notified.",
        avatarColor = Color(0xFFE1F5EE), avatarTextColor = Color(0xFF085041)
    ),
    EmployeeActivity(
        name = "M. Shipanga", role = "Mechanic", totalTasks = 5,
        tasks = listOf(
            TaskEntry("Exhaust inspection", 2),
            TaskEntry("Battery test", 2),
            TaskEntry("Wiper blades", 1)
        ),
        note = "Pending: belt replacement on T-388 — part not yet in stock.",
        avatarColor = Color(0xFFFAEEDA), avatarTextColor = Color(0xFF633806)
    ),
    EmployeeActivity(
        name = "T. Iipinge", role = "Mechanic", totalTasks = 6,
        tasks = listOf(
            TaskEntry("Fuel filter replacement", 3),
            TaskEntry("Differential fluid", 2),
            TaskEntry("Steering fluid top-up", 1)
        ),
        note = "T-204 came in at 287,000 km — flagged for major service review.",
        avatarColor = Color(0xFFEEEDFE), avatarTextColor = Color(0xFF3C3489)
    )
)

val sampleCheckIns = listOf(
    VehicleCheckIn("T-447", "14 Apr 2025", "203,450 km", VehicleCondition.FAIR, "Visible rust on rear bumper, cracked windshield"),
    VehicleCheckIn("T-112", "16 Apr 2025", "175,200 km", VehicleCondition.POOR, "Oil leak reported, engine knocking on start"),
    VehicleCheckIn("T-388", "18 Apr 2025", "142,800 km", VehicleCondition.GOOD, "Routine 15,000 km service"),
    VehicleCheckIn("T-204", "22 Apr 2025", "287,000 km", VehicleCondition.POOR, "Multiple warning lights, worn tyres all round"),
    VehicleCheckIn("T-091", "25 Apr 2025", "98,600 km", VehicleCondition.GOOD, "New vehicle, first service interval")
)

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController) {
    var selectedMechanic by remember { mutableStateOf("All mechanics") }

    Scaffold(
        topBar = { ReportsTopBar() },
        containerColor = Color(0xFFF8F8F6)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Filter bar
            item { FilterBar(selectedMechanic) { selectedMechanic = it } }

            // Metric cards
            item { MetricsRow() }

            // Section: Employee activity
            item {
                SectionHeaderReport(
                    title = "Employee activity",
                    icon = Icons.Outlined.Person
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleEmployees) { emp ->
                        EmployeeCard(emp)
                    }
                }
            }

            // Section: Vehicle check-in log
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeaderReport(
                    title = "Vehicle check-in log",
                    icon = Icons.Outlined.DirectionsCar
                )
            }
            items(sampleCheckIns) { checkIn ->
                CheckInRow(checkIn)
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTopBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Reports",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    "Valentine's Garage",
                    fontSize = 12.sp,
                    color = Color(0xFF888780)
                )
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Outlined.FileDownload,
                        contentDescription = "Export",
                        tint = Color(0xFF534AB7)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEDFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "VL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF3C3489)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

// ── Filter bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(selectedMechanic: String, onMechanicChange: (String) -> Unit) {
    val mechanics = listOf("All mechanics", "J. Kambonde", "P. Nangolo", "M. Shipanga", "T. Iipinge")
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date chip
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Apr 2025", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFFF1EFE8),
                    labelColor = Color(0xFF2C2C2A)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = Color(0xFFD3D1C7),
                    borderWidth = 0.5.dp
                )
            )

            // Mechanic dropdown
            Box {
                FilterChip(
                    selected = selectedMechanic != "All mechanics",
                    onClick = { expanded = true },
                    label = { Text(selectedMechanic, fontSize = 12.sp, maxLines = 1) },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFFF1EFE8),
                        selectedContainerColor = Color(0xFFEEEDFE),
                        labelColor = Color(0xFF2C2C2A),
                        selectedLabelColor = Color(0xFF3C3489)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedMechanic != "All mechanics",
                        borderColor = Color(0xFFD3D1C7),
                        selectedBorderColor = Color(0xFFAFA9EC),
                        borderWidth = 0.5.dp
                    )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    mechanics.forEach { mechanic ->
                        DropdownMenuItem(
                            text = { Text(mechanic, fontSize = 13.sp) },
                            onClick = {
                                onMechanicChange(mechanic)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Metrics row ───────────────────────────────────────────────────────────────

@Composable
fun MetricsRow() {
    val metrics = listOf(
        Triple("Checked in", "14", Color(0xFF1A1A1A)),
        Triple("Completed", "38", Color(0xFF1A1A1A)),
        Triple("Pending", "5", Color(0xFFBA7517)),
        Triple("Mechanics", "4", Color(0xFF1A1A1A))
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.forEach { (label, value, valueColor) ->
            MetricCard(
                label = label,
                value = value,
                valueColor = valueColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0DED5))
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(label, fontSize = 10.sp, color = Color(0xFF888780), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeaderReport(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF888780))
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF888780),
            letterSpacing = 0.8.sp
        )
    }
}

// ── Employee card ─────────────────────────────────────────────────────────────

@Composable
fun EmployeeCard(employee: EmployeeActivity) {
    Card(
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0DED5))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(employee.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "EMP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = employee.avatarTextColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(employee.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                    Text(employee.role, fontSize = 11.sp, color = Color(0xFF888780))
                }
                // Tasks badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEAF3DE))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${employee.totalTasks} tasks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF27500A)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFF0EDE6), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            // Task list
            employee.tasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD3D1C7))
                        )
                        Text(task.label, fontSize = 12.sp, color = Color(0xFF444441))
                    }
                    Text("${task.truckCount} truck${if (task.truckCount > 1) "s" else ""}",
                        fontSize = 11.sp, color = Color(0xFF888780))
                }
            }

            Spacer(Modifier.height(10.dp))
            Divider(color = Color(0xFFF0EDE6), thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // Note
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp).padding(top = 2.dp),
                    tint = Color(0xFFB4B2A9)
                )
                Text(
                    "\"${employee.note}\"",
                    fontSize = 11.sp,
                    color = Color(0xFF888780),
                    fontStyle = FontStyle.Italic,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ── Check-in row ──────────────────────────────────────────────────────────────

@Composable
fun CheckInRow(checkIn: VehicleCheckIn) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0DED5))
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Truck icon box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1EFE8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF5F5E5A)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        checkIn.truckId,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    ConditionBadge(checkIn.condition)
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null,
                            modifier = Modifier.size(11.dp), tint = Color(0xFFB4B2A9))
                        Text(checkIn.date, fontSize = 11.sp, color = Color(0xFF888780))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                        fontSize = 12.sp,
                        color = Color(0xFF5F5E5A),
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ConditionBadge(condition: VehicleCondition) {
    val (bg, text, label) = when (condition) {
        VehicleCondition.GOOD  -> Triple(Color(0xFFEAF3DE), Color(0xFF27500A), "Good")
        VehicleCondition.FAIR  -> Triple(Color(0xFFFAEEDA), Color(0xFF633806), "Fair")
        VehicleCondition.POOR  -> Triple(Color(0xFFFCEBEB), Color(0xFF791F1F), "Poor")
        VehicleCondition.EXCELLENT  -> Triple(Color(0xFFEAF3DE), Color(0xFF27500A), "Good")
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

@Preview(
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFFF8F8F6
)
@Composable
fun ReportsScreenPreview() {
    MaterialTheme {
        ReportsScreen(navController = rememberNavController())
    }
}