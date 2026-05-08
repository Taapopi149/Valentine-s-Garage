package com.example.valentinesgarage.Screens.Manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


// Data class to hold the joined report information
data class EmployeeActivityReport(
    val employeeName: String,
    val truckLicensePlate: String,
    val checkInCondition: String, // From Truck entity
    val taskPerformed: String,    // From Tasks entity
    val completionDate: String
)

// ViewModel to fetch and combine data from Employee, Truck, and Tasks entities
class ReportViewModel : ViewModel() {
    private val repository = ReportRepository()

    // StateFlow to hold the combined report data
    private val _employeeActivityState = MutableStateFlow<List<EmployeeActivityReport>>(emptyList()) // Expose as StateFlow to the UI
    val employeeActivityState: StateFlow<List<EmployeeActivityReport>> = _employeeActivityState // This will hold the combined report data for the UI to observe

    init {
        fetchEmployeeActivityReports()
    }
    

    private fun fetchEmployeeActivityReports() {
        viewModelScope.launch {
            val reports = repository.getEmployeeActivityReports()
            _employeeActivityState.value = reports
        }
    }
}

// Repository to fetch and combine data from the database
class ReportRepository { 
    private val employeeDao = EmployeeDao()
    private val truckDao = TruckDao()
    private val tasksDao = TasksDao()

    suspend fun getEmployeeActivityReports(): List<EmployeeActivityReport> {
        // Fetch all employees, trucks, and tasks
        val employees = employeeDao.getAllEmployees()
        val trucks = truckDao.getAllTrucks()
        val tasks = tasksDao.getAllTasks()

        // Join data based on employee and truck relationships
        return employees.flatMap { employee -> // For each employee, find their tasks and the associated truck information
            val employeeTasks = tasks.filter { it.employeeId == employee.id } // Assuming task has an employeeId to link to the employee
            employeeTasks.map { task -> // For each task, find the associated truck information 
                val truck = trucks.find { it.id == task.truckId } // Assuming task has a truckId to link to the truck
                EmployeeActivityReport( // Create a report entry combining employee, truck, and task information
                    employeeName = employee.name,
                    truckLicensePlate = truck?.licensePlate ?: "Unknown",
                    checkInCondition = truck?.checkInCondition ?: "Unknown",
                    taskPerformed = task.description,
                    completionDate = task.completionDate
                )
            }
        }
    }
}

// Composable function to display the detailed manager report screen
@Composable
fun DetailedManagerReportScreen( // This screen will display a comprehensive report of employee activities and vehicle conditions
    viewModel: ReportViewModel = viewModel()
) {
    val detailedReports by viewModel.employeeActivityState.collectAsState() // Collect the combined report data from the ViewModel

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Employee Activity & Vehicle Conditions",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(detailedReports) { report ->
                ActivityCard(report)
            }
        }
    }
}

// Composable function to display each activity report in a card format
@Composable
fun ActivityCard(report: EmployeeActivityReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Employee: ${report.employeeName}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = report.completionDate, fontSize = 12.sp)
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(text = "Vehicle: ${report.truckLicensePlate}", fontWeight = FontWeight.SemiBold)
            
            // This satisfies the requirement to see the condition at check-in
            Text(
                text = "Check-in Condition: ${report.checkInCondition}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Task Completed: ${report.taskPerformed}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

