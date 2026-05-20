package com.example.valentinesgarage.Manager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Screens.Manager.ManagerViewModel
import com.example.valentinesgarage.Screens.Manager.ViewFactory.ManagerViewModelFactory
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesScreen

// ─── Data Model ───────────────────────────────────────────────────────────────

data class Mechanic(
    val employeeId: String,
    val firstName:  String,
    val lastName:   String,
    val role:       String,
    val shift:      String,
    val isActive:   Boolean = true
) {
    val fullName get() = "$firstName $lastName"
    val initials get() = "${firstName.firstOrNull()?.uppercase() ?: ""}${lastName.firstOrNull()?.uppercase() ?: ""}"
}

// ─── Bottom Nav Routes ────────────────────────────────────────────────────────

sealed class GarageNavRoute(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : GarageNavRoute("dashboard", "Home", Icons.Default.Home)
    object Trucks    : GarageNavRoute("trucks",    "Trucks", Icons.Default.DirectionsCar)
    object Mechanics : GarageNavRoute("mechanics", "Staff",  Icons.Default.Person)
    object Search    : GarageNavRoute("search",    "Search", Icons.Default.Search)
}

// ─── Root Screen with Bottom Bar ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(navController: NavController, userDao: UserDao) {

    val viewModel: ManagerViewModel = viewModel(
        factory = ManagerViewModelFactory(userDao)
    )
    val employees by viewModel.employees.collectAsState()

    var selectedRoute by remember { mutableStateOf(GarageNavRoute.Dashboard.route) }

    val navItems = listOf(
        GarageNavRoute.Dashboard,
        GarageNavRoute.Trucks,
        GarageNavRoute.Mechanics,
        GarageNavRoute.Search,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Valentine's Garage", fontWeight = FontWeight.Bold) },
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
                    IconButton(onClick = { navController.navigate("addEmployee") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Employee", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedRoute == item.route,
                        onClick  = { selectedRoute = item.route },
                        icon     = { Icon(item.icon, contentDescription = item.label) },
                        label    = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedRoute) {
                GarageNavRoute.Dashboard.route -> DashboardTab(
                    employees     = employees,
                    onAddMechanic = { navController.navigate("addEmployee") },
                    onReportClick = { navController.navigate("reports") },
                    onViewTrucks  = { selectedRoute = GarageNavRoute.Trucks.route },
                    onViewStaff   = { selectedRoute = GarageNavRoute.Mechanics.route },
                    onViewJobs    = { navController.navigate("MechanicTaskList") }
                )
                GarageNavRoute.Trucks.route -> ActiveVehiclesScreen(navController)
                GarageNavRoute.Mechanics.route -> MechanicsTab(
                    mechanics     = employees,
                    onAddMechanic = { navController.navigate("addEmployee") }
                )
                GarageNavRoute.Search.route -> SearchTab(mechanics = employees)
            }
        }
    }
}

// ─── Dashboard Tab ────────────────────────────────────────────────────────────

@Composable
private fun DashboardTab(
    employees: List<Mechanic>,
    onAddMechanic: () -> Unit,
    onReportClick: () -> Unit,
    onViewTrucks:  () -> Unit,
    onViewStaff:   () -> Unit,
    onViewJobs:    () -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Staff",
                    value    = "${employees.size}",
                    icon     = Icons.Default.People,
                    color    = MaterialTheme.colorScheme.primary,
                    onClick  = onViewStaff
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Trucks",
                    value    = "4",
                    icon     = Icons.Default.DirectionsCar,
                    color    = Color(0xFF22C55E),
                    onClick  = onViewTrucks
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Jobs",
                    value    = "1",
                    icon     = Icons.Default.Build,
                    color    = Color(0xFFF59E0B),
                    onClick  = onViewJobs
                )
            }
        }

        // Quick Actions
        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    label    = "Add Staff",
                    icon     = Icons.Default.PersonAdd,
                    color    = MaterialTheme.colorScheme.primary,
                    onClick  = onAddMechanic
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    label    = "Reports",
                    icon     = Icons.Default.BarChart,
                    color    = Color(0xFFF59E0B),
                    onClick  = onReportClick
                )
            }
        }

        // Recent Staff
        item {
            Text("Recent Staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        if (employees.isEmpty()) {
            item {
                Text("No staff members found.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            items(employees.take(3)) { mechanic ->
                MechanicListItem(mechanic = mechanic)
            }
        }
    }
}

@Composable
private fun MechanicsTab(mechanics: List<Mechanic>, onAddMechanic: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("All Staff", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = onAddMechanic) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }
        if (mechanics.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No staff registered yet.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mechanics) { mechanic -> MechanicListItem(mechanic = mechanic) }
            }
        }
    }
}

@Composable
private fun SearchTab(mechanics: List<Mechanic>) {
    var query by remember { mutableStateOf("") }
    val filtered = mechanics.filter { it.fullName.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Staff...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { mechanic -> MechanicListItem(mechanic = mechanic) }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickActionCard(modifier: Modifier, label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun MechanicListItem(mechanic: Mechanic) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(mechanic.initials, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(mechanic.fullName, fontWeight = FontWeight.SemiBold)
                Text("${mechanic.role.replaceFirstChar { it.uppercase() }} • ${mechanic.shift}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
