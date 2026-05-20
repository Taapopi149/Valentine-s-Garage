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
import androidx.compose.ui.text.font.FontFamily
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
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Data.Entities.Truck
import com.example.valentinesgarage.Data.Entities.User
import com.example.valentinesgarage.Screens.Manager.ViewFactory.ManagerDashboardViewModelFactory
import com.example.valentinesgarage.Screens.Vehicles.TruckStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

// ─── UI models ────────────────────────────────────────────────────────────────

data class Mechanic(
    val employeeId: String,
    val firstName:  String,
    val lastName:   String,
    val role:       String,
    val shift:      String,
    val isActive:   Boolean = true
) {
    val fullName get() = "$firstName $lastName"
    val initials get() = "${firstName.first()}${lastName.first()}"
}

// Unified search result — either a user or a truck
sealed class SearchResult {
    data class UserResult(val user: Mechanic)      : SearchResult()
    data class TruckResult(val truck: TruckSearchItem) : SearchResult()
}

data class TruckSearchItem(
    val id:           Int,
    val licencePlate: String,
    val driverName:   String,
    val status:       TruckStatus
)

// ─── Bottom Nav Routes ────────────────────────────────────────────────────────

sealed class GarageNavRoute(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : GarageNavRoute("dashboard", "Dashboard", Icons.Default.Home)
    object Mechanics : GarageNavRoute("mechanics", "Mechanics", Icons.Default.Person)
    object Search    : GarageNavRoute("search",    "Search",    Icons.Default.Search)
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ManagerDashboardViewModel(
    private val userDao:  UserDao,
    private val truckDao: TruckDao,
    private val tasksDao: TasksDao
) : ViewModel() {

    // ── Stats ─────────────────────────────────────────────────────────────────

    val employeeCount: StateFlow<Int> =
        userDao.getAllMechanics()
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val truckCount: StateFlow<Int> =
        truckDao.getTruckCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeTaskCount: StateFlow<Int> =
        tasksDao.getActiveTaskCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Mechanics list ────────────────────────────────────────────────────────

    val mechanics: StateFlow<List<Mechanic>> =
        userDao.getAllMechanics()
            .map { list -> list.map { it.toMechanic() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Search ────────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Debounce so DB isn't hit on every keystroke
    val searchResults: StateFlow<List<SearchResult>> =
        _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) flowOf(emptyList())
                else combine(
                    userDao.searchUsers(query).map { users ->
                        users.map { SearchResult.UserResult(it.toMechanic()) }
                    },
                    truckDao.searchTrucks(query).map { trucks ->
                        trucks.map { SearchResult.TruckResult(it.toTruckSearchItem()) }
                    }
                ) { userResults, truckResults ->
                    userResults + truckResults
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun clearSearch() { _searchQuery.value = "" }
}

// is Active does not work (will break the whole page for some reason)

// ─── Mappers ──────────────────────────────────────────────────────────────────
private fun User.toMechanic() = Mechanic(
    employeeId = employeeId,
    firstName  = firstName,
    lastName   = lastName,
    role       = role,
    shift      = shift ?: "N/A",
    isActive   = true   // add an isActive column to User if you need real values
)

private fun Truck.toTruckSearchItem() = TruckSearchItem(
    id           = truckId,
    licencePlate = licencePlate,
    driverName   = DriverName,
    status       = truckStatus
)

// ─── Root Screen ──────────────────────────────────────────────────────────────

@Composable
fun ManagerDashboardScreen(
    navController: NavController,
    userDao:       UserDao,
    truckDao:      TruckDao,
    tasksDao:      TasksDao
) {
    val viewModel: ManagerDashboardViewModel = viewModel(
        factory = ManagerDashboardViewModelFactory(userDao, truckDao, tasksDao)
    )

    var selectedRoute by remember { mutableStateOf(GarageNavRoute.Dashboard.route) }

    val navItems = listOf(
        GarageNavRoute.Dashboard,
        GarageNavRoute.Mechanics,
        GarageNavRoute.Search,
    )

    val mechanics       by viewModel.mechanics.collectAsState()
    val employeeCount   by viewModel.employeeCount.collectAsState()
    val truckCount      by viewModel.truckCount.collectAsState()
    val activeTaskCount by viewModel.activeTaskCount.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val searchResults   by viewModel.searchResults.collectAsState()

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
                    mechanics       = mechanics,
                    employeeCount   = employeeCount,
                    truckCount      = truckCount,
                    activeTaskCount = activeTaskCount,
                    onAddMechanic   = { navController.navigate("addEmployee") },
                    onReportClick   = { navController.navigate("Report") }
                )
                GarageNavRoute.Mechanics.route -> MechanicsTab(
                    mechanics     = mechanics,
                    onAddMechanic = { navController.navigate("addEmployee") }
                )
                GarageNavRoute.Search.route -> SearchTab(
                    query         = searchQuery,
                    results       = searchResults,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClear       = viewModel::clearSearch
                )
            }
        }
    }
}

// ─── Dashboard Tab ────────────────────────────────────────────────────────────

@Composable
private fun DashboardTab(
    mechanics:       List<Mechanic>,
    employeeCount:   Int,
    truckCount:      Int,
    activeTaskCount: Int,
    onAddMechanic:   () -> Unit,
    onReportClick:   () -> Unit
) {
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
                Text(
                    text       = "Valentine's Garage",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAddMechanic) {
                    Icon(
                        imageVector        = Icons.Default.Add,
                        contentDescription = "Add Employee",
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Stats — live from DB
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Employees",
                    value    = employeeCount.toString(),
                    icon     = Icons.Default.Person,
                    color    = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Trucks",
                    value    = truckCount.toString(),
                    icon     = Icons.Default.Build,
                    color    = Color(0xFF22C55E)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "Active Tasks",
                    value    = activeTaskCount.toString(),
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
                    onClick  = onReportClick
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
                TextButton(onClick = {}) { Text("See all") }
            }
        }

        // First 3 mechanics from DB
        items(mechanics.take(3)) { mechanic ->
            MechanicListItem(mechanic = mechanic)
        }

        if (mechanics.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No employees found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

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
                text       = "All Employees",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(onClick = onAddMechanic) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        if (mechanics.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No employees yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mechanics, key = { it.employeeId }) { mechanic ->
                    MechanicListItem(mechanic = mechanic)
                }
            }
        }
    }
}

// ─── Search Tab ───────────────────────────────────────────────────────────────

@Composable
private fun SearchTab(
    query:         String,
    results:       List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onClear:       () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = "Search",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Search employees or trucks…") },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon  = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape      = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (query.isNotBlank()) {
            Text(
                text  = "${results.size} result(s) found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            items(results) { result ->
                when (result) {
                    is SearchResult.UserResult  -> MechanicListItem(mechanic = result.user)
                    is SearchResult.TruckResult -> TruckSearchCard(truck = result.truck)
                }
            }

            if (query.isNotBlank() && results.isEmpty()) {
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
                                text  = "No results found",
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

// ─── Truck search card ────────────────────────────────────────────────────────

@Composable
private fun TruckSearchCard(truck: TruckSearchItem) {
    val (statusColor, statusBg) = when (truck.status) {
        TruckStatus.WAITING     -> Color(0xFF1D4ED8) to Color(0xFFEFF6FF)
        TruckStatus.IN_PROGRESS -> Color(0xFFB45309) to Color(0xFFFFFBEB)
        TruckStatus.DONE        -> Color(0xFF065F46) to Color(0xFFECFDF5)
    }

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
            // Truck icon box
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Build,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.secondary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = truck.licencePlate,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text     = truck.driverName,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status badge
            Surface(shape = RoundedCornerShape(20.dp), color = statusBg) {
                Text(
                    text       = truck.status.label,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Reusable components (unchanged) ─────────────────────────────────────────

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
            modifier            = Modifier.padding(12.dp).fillMaxWidth(),
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
        Scaffold(
            bottomBar = {
                NavigationBar {
                    listOf("Dashboard", "Mechanics", "Search").forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = i == 0,
                            onClick  = {},
                            icon     = { Icon(Icons.Default.Home, null) },
                            label    = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                DashboardTab(
                    mechanics       = listOf(
                        Mechanic("MG001", "James", "Mokoena",  "Senior Mechanic", "Morning",   true),
                        Mechanic("MG002", "Sarah", "Ndlovu",   "Mechanic",        "Afternoon", true),
                        Mechanic("MG003", "Peter", "Dlamini",  "Mechanic",        "Morning",   false),
                    ),
                    employeeCount   = 5,
                    truckCount      = 4,
                    activeTaskCount = 3,
                    onAddMechanic   = {},
                    onReportClick   = {}
                )
            }
        }
    }
}