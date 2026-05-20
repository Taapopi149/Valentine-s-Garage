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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.valentinesgarage.R

// --- Data models (replace with real data from your ViewModel) ---

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
enum class TaskStatus { PENDING, IN_PROGRESS, DONE }
enum class TruckStatus { AVAILABLE, IN_SERVICE, WAITING }

// --- Sample data (swap with ViewModel state) ---

private val sampleTasks = listOf(
    RecentTask("T001", "Oil Change & Filter Replacement", "NAM 1234", TaskPriority.HIGH, TaskStatus.IN_PROGRESS),
    RecentTask("T002", "Brake Inspection", "WDH 5678", TaskPriority.MEDIUM, TaskStatus.PENDING),
    RecentTask("T003", "Tyre Rotation", "NAM 9012", TaskPriority.LOW, TaskStatus.DONE),
    RecentTask("T004", "Engine Diagnostics", "WDH 3456", TaskPriority.HIGH, TaskStatus.PENDING),
)

private val sampleTrucks = listOf(
    TruckWithTasks("NAM 1234", "Volvo FH16", 2, "Oil Change", TruckStatus.IN_SERVICE),
    TruckWithTasks("WDH 5678", "Mercedes Actros", 1, "Brake Inspection", TruckStatus.WAITING),
    TruckWithTasks("NAM 9012", "MAN TGX", 0, null, TruckStatus.AVAILABLE),
    TruckWithTasks("WDH 3456", "DAF XF", 3, "Engine Diagnostics", TruckStatus.IN_SERVICE),
)

// --- Main Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeHomePage(navController: NavController) {
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
                            imageVector = Icons.Default.AccountCircle,
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {

            // --- Top Cards Row ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CardDesign(
                        imageRs = R.drawable.activetruckimg,
                        title = "Trucks in Garage",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("ActiveVehicle") }
                    )
                    CardDesign(
                        imageRs = R.drawable.taskspic,
                        title = "Tasks",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("MechanicTaskList") }
                    )
                }
            }

            // --- Check In Button ---
            item {
                Button(
                    onClick = { navController.navigate("TruckCheckIn") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check In Truck",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // --- Recent Tasks Section ---
            item {
                SectionHeader(
                    title = "Recent Tasks",
                    onSeeAll = { navController.navigate("MechanicTaskList") }
                )
            }

            items(sampleTasks.take(4)) { task ->
                RecentTaskCard(task = task)
            }

            // --- Available Trucks Section ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(
                    title = "Trucks & Their Tasks",
                    onSeeAll = { navController.navigate("ActiveVehicle") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(sampleTrucks) { truck ->
                        TruckTaskCard(truck = truck)
                    }
                }
            }
        }
    }
}

// --- Section Header ---

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A)
        )
        TextButton(onClick = onSeeAll) {
            Text(
                text = "See all",
                fontSize = 13.sp,
                color = Color(0xFF555555)
            )
        }
    }
}

// --- Recent Task Card ---

@Composable
fun RecentTaskCard(task: RecentTask) {
    val (statusColor, statusLabel, statusIcon) = when (task.status) {
        TaskStatus.IN_PROGRESS -> Triple(Color(0xFF1565C0), "In Progress", Icons.Default.Build)
        TaskStatus.PENDING     -> Triple(Color(0xFFE65100), "Pending", Icons.Default.Schedule)
        TaskStatus.DONE        -> Triple(Color(0xFF2E7D32), "Done", Icons.Default.CheckCircle)
    }

    val priorityColor = when (task.priority) {
        TaskPriority.HIGH   -> Color(0xFFD32F2F)
        TaskPriority.MEDIUM -> Color(0xFFF57C00)
        TaskPriority.LOW    -> Color(0xFF388E3C)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = task.truckPlate,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            // Right side badges
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                // Priority dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Text(
                        text = task.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = priorityColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// --- Truck Task Card (horizontal scroll) ---

@Composable
fun TruckTaskCard(truck: TruckWithTasks) {
    val (statusColor, statusLabel) = when (truck.status) {
        TruckStatus.IN_SERVICE -> Pair(Color(0xFF1565C0), "In Service")
        TruckStatus.WAITING    -> Pair(Color(0xFFE65100), "Waiting")
        TruckStatus.AVAILABLE  -> Pair(Color(0xFF2E7D32), "Available")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(175.dp)
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Truck icon + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF444444),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Plate + model
            Text(
                text = truck.plate,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = truck.model,
                fontSize = 12.sp,
                color = Color(0xFF888888),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))

            // Task info
            if (truck.currentTask != null) {
                Text(
                    text = "Current Task",
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = truck.currentTask,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "No active tasks",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task count chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Text(
                    text = "${truck.taskCount} task${if (truck.taskCount != 1) "s" else ""} total",
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// --- Existing CardDesign (unchanged) ---

@Composable
fun CardDesign(
    imageRs: Int,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = cardElevation(8.dp),
        modifier = modifier.fillMaxHeight()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRs),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomepagePreview() {
    EmployeeHomePage(navController = rememberNavController())
}