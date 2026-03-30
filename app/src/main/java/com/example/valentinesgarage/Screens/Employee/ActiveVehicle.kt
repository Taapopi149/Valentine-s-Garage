package com.example.valentinesgarage.Screens.Vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

// ─── Data ─────────────────────────────────────────────────────────────────────

enum class TruckStatus(val label: String) {
    WAITING("Waiting"),
    IN_PROGRESS("In Progress"),
    DONE("Done")
}

data class ActiveTruck(
    val id: String,
    val licencePlate: String,
    val driverName: String,
    val checkInTime: String,
    val status: TruckStatus
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ActiveVehiclesViewModel : ViewModel() {

    // TODO: replace with Firestore / Room live data
    val trucks by mutableStateOf(
        listOf(
            ActiveTruck("1", "N 12345 W", "Johannes Shikongo", "09:00", TruckStatus.IN_PROGRESS),
            ActiveTruck("2", "N 78900 W", "Petrus Hamutenya",  "08:30", TruckStatus.WAITING),
            ActiveTruck("3", "N 55231 W", "Maria Shipanga",    "07:45", TruckStatus.DONE),
        )
    )

    var selectedFilter by mutableStateOf<TruckStatus?>(null)
        private set

    fun onFilterChange(status: TruckStatus?) { selectedFilter = status }

    fun filteredTrucks(): List<ActiveTruck> =
        if (selectedFilter == null) trucks
        else trucks.filter { it.status == selectedFilter }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveVehiclesScreen(
    navController: NavController,
    viewModel: ActiveVehiclesViewModel = viewModel()
) {
    val filtered = viewModel.filteredTrucks()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Active Vehicles")
                        Text(
                            text = "${viewModel.trucks.size} trucks on the floor today",
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

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // ── Filter chips ──────────────────────────────────────────────
            StatusFilterRow(
                selected = viewModel.selectedFilter,
                onSelect = viewModel::onFilterChange
            )

            HorizontalDivider(thickness = 0.5.dp)

            // ── Truck list ────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trucks match this filter",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { truck ->
                        TruckCard(
                            truck = truck,
                            onClick = {
                                // Navigates to the detail screen, passing the truck id
                                navController.navigate( "truckDetail/${truck.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Filter row ───────────────────────────────────────────────────────────────

@Composable
fun StatusFilterRow(
    selected: TruckStatus?,
    onSelect: (TruckStatus?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All", fontSize = 12.sp) },
            shape = RoundedCornerShape(20.dp)
        )

        TruckStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(status.label, fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ─── Truck card ───────────────────────────────────────────────────────────────

@Composable
fun TruckCard(
    truck: ActiveTruck,
    onClick: () -> Unit
) {
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
            // Left: plate + meta
            Column(modifier = Modifier.weight(1f)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Licence plate
                    Text(
                        text = truck.licencePlate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )

                    // Status badge
                    StatusBadge(truck.status)
                }

                Spacer(Modifier.height(5.dp))

                // Driver + check-in time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = truck.driverName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "·",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = truck.checkInTime,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
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

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Text(
            text = status.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun ActiveVehiclesPreview() {
    ActiveVehiclesScreen(navController = rememberNavController())
}