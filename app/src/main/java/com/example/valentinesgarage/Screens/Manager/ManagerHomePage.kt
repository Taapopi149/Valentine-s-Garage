package com.example.valentinesgarage.Manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// ─── Data Model ───────────────────────────────────────────────────────────────

data class Mechanic(
    val employeeId: String,
    val firstName:  String,
    val lastName:   String,
    val role:       String,       // e.g. "Senior Mechanic", "Apprentice"
    val shift:      String,       // e.g. "Morning", "Afternoon"
    val isActive:   Boolean = true
) {
    val fullName get() = "$firstName $lastName"
    val initials get() = "${firstName.first()}${lastName.first()}"
}

// ─── Bottom Nav Routes ────────────────────────────────────────────────────────

sealed class GarageNavRoute(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : GarageNavRoute("dashboard", "Dashboard", Icons.Default.Home)
    object Mechanics : GarageNavRoute("mechanics", "Mechanics", Icons.Default.Person)
    object Search    : GarageNavRoute("search",    "Search",    Icons.Default.Search)
}

// ─── Sample Data ────────────────────────────────────────────────────────────── REMOVE WHEN IMPLIMENTING ROOM

private val sampleMechanics = listOf(
    Mechanic("MG001", "James",  "Mokoena",   "Senior Mechanic", "Morning",   true),
    Mechanic("MG002", "Sarah",  "Ndlovu",    "Mechanic",        "Afternoon", true),
    Mechanic("MG003", "Peter",  "Dlamini",   "Mechanic",        "Morning",   false),
    Mechanic("MG004", "Anna",   "Shipanga",  "Apprentice",      "Morning",   true),
    Mechanic("MG005", "David",  "Hamutenya", "Senior Mechanic", "Afternoon", true),
)

// ─── Root Screen with Bottom Bar ──────────────────────────────────────────────

@Composable
fun ManagerDashboardScreen(navController: NavController) {

    var selectedRoute by remember { mutableStateOf(GarageNavRoute.Dashboard.route) }

    val navItems = listOf(
        GarageNavRoute.Dashboard,
        GarageNavRoute.Mechanics,
        GarageNavRoute.Search,
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedRoute == item.route,
                        onClick  = { selectedRoute = item.route },
                        icon     = { Icon(item.icon, contentDescription = item.label) },
                        label    = { Text(item.label) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedRoute) {
                GarageNavRoute.Dashboard.route -> DashboardTab(
                    onAddMechanic = { navController.navigate("addEmployee") } // Navigate to AddEmployee
                )
                GarageNavRoute.Mechanics.route -> MechanicsTab(
                    mechanics     = sampleMechanics,
                    onAddMechanic = { navController.navigate("addEmployee") } // Navigate to AddEmployee
                )
                GarageNavRoute.Search.route -> SearchTab(mechanics = sampleMechanics)
            }
        }
    }
}

// ─── Dashboard Tab ────────────────────────────────────────────────────────────

@Composable
private fun DashboardTab(onAddMechanic: () -> Unit) {

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {

                    Text(
                        text       = "Valentine's Garage",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onAddMechanic) {
                    Icon(
                        imageVector        = Icons.Default.Add,
                        contentDescription = "Add Mechanic",
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Stats — garage relevant
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Mechanics",
                    value    = "5",
                    icon     = Icons.Default.Person,
                    color    = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Trucks",
                    value    = "4",
                    icon     = Icons.Default.Build,
                    color    = Color(0xFF22C55E)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Tasks",
                    value    = "1",
                    icon     = Icons.Default.DateRange,
                    color    = Color(0xFFF59E0B)
                )
            }
        }

        // Quick Actions
        item {
            Text(
                text       = "Quick Actions",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    label    = "Add Mechanic",
                    icon     = Icons.Default.PersonAdd,
                    color    = MaterialTheme.colorScheme.primary,
                    onClick  = onAddMechanic
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    label    = "View Jobs",
                    icon     = Icons.Default.List,
                    color    = Color(0xFF3B82F6),
                    onClick  = {}
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    label    = "Reports",
                    icon     = Icons.Default.Info,
                    color    = Color(0xFFF59E0B),
                    onClick  = {}
                )
            }
        }

        // Recent Mechanics header
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Recent Mechanics",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {}) {
                    Text("See all")
                }
            }
        }

        // First 3 mechanics preview
        items(sampleMechanics.take(3)) { mechanic ->
            MechanicListItem(mechanic = mechanic)
        }

        // Add Mechanic button
        item {
            Button(
                onClick  = onAddMechanic,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add New Mechanic")
            }
        }
    }
}

// ─── Mechanics Tab ────────────────────────────────────────────────────────────

@Composable
private fun MechanicsTab(
    mechanics:     List<Mechanic>,
    onAddMechanic: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "All Mechanics",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(onClick = onAddMechanic) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(mechanics) { mechanic ->
                MechanicListItem(mechanic = mechanic)
            }
        }
    }
}

// ─── Search Tab ───────────────────────────────────────────────────────────────

@Composable
private fun SearchTab(mechanics: List<Mechanic>) {

    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) mechanics
        else mechanics.filter { m ->
            m.fullName.contains(query, ignoreCase = true)   ||
                    m.employeeId.contains(query, ignoreCase = true) ||
                    m.role.contains(query, ignoreCase = true)       ||
                    m.shift.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text       = "Search Mechanics",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Search by name or ID…") },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon  = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { //Query Geos here ROOM
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape      = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (query.isNotBlank()) {
            Text(
                text  = "${filtered.size} result(s) found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { mechanic ->
                MechanicListItem(mechanic = mechanic)
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Default.Search,
                                contentDescription = null,
                                modifier           = Modifier.size(48.dp),
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text  = "No mechanics found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label:    String,
    value:    String,
    icon:     ImageVector,
    color:    Color
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    label:    String,
    icon:     ImageVector,
    color:    Color,
    onClick:  () -> Unit
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        onClick  = onClick
    ) {
        Column(
            modifier            = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = color,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MechanicListItem(mechanic: Mechanic) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = mechanic.initials,
                    color      = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name, role, employee ID and shift
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = mechanic.fullName,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = "${mechanic.role} · ${mechanic.shift} Shift",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = mechanic.employeeId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // On Shift / Off Duty badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (mechanic.isActive) Color(0xFF22C55E).copy(alpha = 0.15f)
                else Color(0xFFF59E0B).copy(alpha = 0.15f)
            ) {
                Text(
                    text       = if (mechanic.isActive) "On Shift" else "Off Duty",
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = if (mechanic.isActive) Color(0xFF16A34A) else Color(0xFFD97706),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ManagerDashboardScreenPreview() {
    MaterialTheme {
        ManagerDashboardScreen(navController = rememberNavController())
    }
}