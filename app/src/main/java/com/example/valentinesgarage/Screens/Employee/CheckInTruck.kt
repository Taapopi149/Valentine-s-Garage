package com.example.valentinesgarage.Screens.CheckIn

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.valentinesgarage.Data.DAO.NotesDao
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Data.Entities.Notes
import com.example.valentinesgarage.Data.Entities.Tasks
import com.example.valentinesgarage.Data.Entities.Truck
import com.example.valentinesgarage.Data.Entities.User
import com.example.valentinesgarage.Screens.Employee.ViewModelFactory.CheckInViewModelFactory
import com.example.valentinesgarage.Screens.Vehicles.TruckStatus
import kotlinx.coroutines.launch
import java.io.File

// ─── Data ─────────────────────────────────────────────────────────────────────

enum class VehicleCondition(val label: String, val filledSegments: Int) {
    EXCELLENT("Excellent", 4),
    GOOD("Good", 3),
    FAIR("Fair", 2),
    POOR("Poor", 1)
}

data class AssignedTask(
    val id: String,
    val description: String,
    val assignedTo: String
)

data class TruckCheckInForm(
    val licencePlate: String = "",
    val driverName: String = "",
    val odometer: String = "",
    val condition: VehicleCondition = VehicleCondition.GOOD,
    val photoUris: List<Uri> = emptyList(),
    val notes: String = "",
    val assignedTasks: List<AssignedTask> = emptyList()
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class CheckInViewModel(
    private val truckDao: TruckDao,
    private val notesDao: NotesDao,
    private val tasksDao: TasksDao,
    private val userDao: UserDao
) : ViewModel() {

    var form by mutableStateOf(TruckCheckInForm())
        private set

    private var mechanicUsers by mutableStateOf<List<User>>(emptyList())

    val availableMechanics: List<String>
        get() = mechanicUsers.map { "${it.firstName} ${it.lastName}" }

    init {
        loadMechanics()
    }

    private fun loadMechanics() {
        viewModelScope.launch {
            mechanicUsers = userDao.getUsersByRole("Mechanic")
        }
    }

    fun onLicencePlateChange(value: String) { form = form.copy(licencePlate = value) }
    fun onDriverNameChange(value: String)   { form = form.copy(driverName = value) }
    fun onOdometerChange(value: String)     { form = form.copy(odometer = value.filter { it.isDigit() }) }
    fun onConditionChange(c: VehicleCondition) { form = form.copy(condition = c) }
    fun onPhotosAdded(uris: List<Uri>)      { form = form.copy(photoUris = (form.photoUris + uris).take(5)) }
    fun onPhotoTaken(uri: Uri)              { form = form.copy(photoUris = (form.photoUris + uri).take(5)) }
    fun onNotesChange(value: String)        { form = form.copy(notes = value) }

    // ── Task assignment ───────────────────────────────────────────────────────

    fun addTask(description: String, mechanic: String) {
        if (description.isBlank() || mechanic.isBlank()) return
        val task = AssignedTask(
            id = "task_${System.currentTimeMillis()}",
            description = description.trim(),
            assignedTo = mechanic
        )
        form = form.copy(assignedTasks = form.assignedTasks + task)
    }

    fun removeTask(taskId: String) {
        form = form.copy(assignedTasks = form.assignedTasks.filter { it.id != taskId })
    }

    // ── Camera helper ─────────────────────────────────────────────────────────

    fun createCameraUri(context: Context): Uri {
        val imageFile = File.createTempFile("truck_", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    fun submitCheckIn(onSuccess: () -> Unit) {
        if (!isFormValid()) return

        viewModelScope.launch {
            // 1. Insert Truck, get back the generated ID
            val truck = Truck(
                licencePlate = form.licencePlate,
                DriverName   = form.driverName,
                Odmeter      = form.odometer.toIntOrNull() ?: 0,
                Condition    = form.condition,
                photoUris    = form.photoUris.joinToString("|") { it.toString() }
            )
            val newTruckId: Long = truckDao.insertTruck(truck)

            // 2. Insert Notes if present
            if (form.notes.isNotBlank()) {
                notesDao.insertNote(
                    Notes(
                        truckIdOwner = newTruckId.toInt(),
                        noteText     = form.notes
                    )
                )
            }

            // 3. Insert each assigned task
            form.assignedTasks.forEach { assignedTask ->
                val mechanic = mechanicUsers.firstOrNull { user ->
                    "${user.firstName} ${user.lastName}" == assignedTask.assignedTo
                }
                if (mechanic != null) {
                    tasksDao.insertTask(
                        Tasks(
                            description  = assignedTask.description,
                            employeeIdFk = mechanic.employeeId,
                            truckIdOwner = newTruckId.toInt(),
                            status       = "Pending"
                        )
                    )
                }
            }

            // 4. All inserts done — now navigate back
            onSuccess()
        }
    }

    fun isFormValid(): Boolean =
        form.licencePlate.isNotBlank() &&
                form.driverName.isNotBlank() &&
                form.odometer.isNotBlank()
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckCheckInScreen(
    navController: NavController, truckDao: TruckDao, notesDao: NotesDao, tasksDao: TasksDao, userDao: UserDao
) {

    val viewModel: CheckInViewModel = viewModel(
        factory = CheckInViewModelFactory(
            truckDao,notesDao,tasksDao, userDao
        )
    )


    val form     = viewModel.form
    val context  = LocalContext.current
    val mechanics = viewModel.availableMechanics

    // Camera state
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSheet   by remember { mutableStateOf(false) }

    // Task input — local until added to form
    var taskInput            by remember { mutableStateOf("") }
    // Safe init: empty string until mechanics load, then re-initialises with first entry
    var selectedMechanic     by remember(mechanics) { mutableStateOf(mechanics.firstOrNull() ?: "") }
    var mechanicDropdownOpen by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let { viewModel.onPhotoTaken(it) }
        pendingCameraUri = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> viewModel.onPhotosAdded(uris) }

    // ── Photo source bottom sheet ─────────────────────────────────────────────
    if (showPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add photo",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ListItem(
                    headlineContent = { Text("Take a photo") },
                    leadingContent  = { Icon(Icons.Outlined.CameraAlt, null) },
                    modifier = Modifier.clickable {
                        showPhotoSheet = false
                        val uri = viewModel.createCameraUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                )
                ListItem(
                    headlineContent = { Text("Choose from gallery") },
                    leadingContent  = { Icon(Icons.Outlined.PhotoLibrary, null) },
                    modifier = Modifier.clickable {
                        showPhotoSheet = false
                        galleryLauncher.launch("image/*")
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Truck Check-In") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ── Vehicle info ──────────────────────────────────────────────────
            FormSectionLabel("Vehicle info")

            FieldLabel("Licence plate")
            OutlinedTextField(
                value         = form.licencePlate,
                onValueChange = viewModel::onLicencePlateChange,
                placeholder   = { Text("e.g. N 12345 W") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            FieldLabel("Driver name")
            OutlinedTextField(
                value         = form.driverName,
                onValueChange = viewModel::onDriverNameChange,
                placeholder   = { Text("Full name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            FieldLabel("Odometer (km)")
            OutlinedTextField(
                value          = form.odometer,
                onValueChange  = viewModel::onOdometerChange,
                placeholder    = { Text("e.g. 148302") },
                singleLine     = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            FieldLabel("Vehicle condition")
            ConditionSelector(
                selected = form.condition,
                onSelect = viewModel::onConditionChange
            )

            Spacer(Modifier.height(8.dp))

            FieldLabel("Photos (up to 5)")
            PhotoPicker(
                uris      = form.photoUris,
                onAddPhotos = { showPhotoSheet = true }
            )

            Spacer(Modifier.height(8.dp))

            FieldLabel("Notes / damage description")
            OutlinedTextField(
                value         = form.notes,
                onValueChange = viewModel::onNotesChange,
                placeholder   = { Text("Describe any scratches, damage, etc.") },
                minLines      = 3,
                maxLines      = 5,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )

            // ── Task assignment ───────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            FormSectionLabel("Assign tasks")

            FieldLabel("Task description")
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment    = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = taskInput,
                    onValueChange = { taskInput = it },
                    placeholder   = { Text("e.g. Oil & filter change") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(10.dp)
                )
                Button(
                    onClick = {
                        viewModel.addTask(taskInput, selectedMechanic)
                        taskInput = ""
                    },
                    // Disabled if no task text OR no mechanic loaded yet
                    enabled = taskInput.isNotBlank() && selectedMechanic.isNotBlank(),
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(
                        containerColor        = Color(0xFF1A1A1A),
                        disabledContainerColor = Color(0xFFCCCCCC)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text("Add", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            FieldLabel("Assign to mechanic")
            ExposedDropdownMenuBox(
                // Guard: don't open if the list hasn't loaded yet
                expanded        = mechanicDropdownOpen && mechanics.isNotEmpty(),
                onExpandedChange = { if (mechanics.isNotEmpty()) mechanicDropdownOpen = it }
            ) {
                OutlinedTextField(
                    value         = if (mechanics.isEmpty()) "Loading…" else selectedMechanic,
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = mechanicDropdownOpen)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded        = mechanicDropdownOpen && mechanics.isNotEmpty(),
                    onDismissRequest = { mechanicDropdownOpen = false }
                ) {
                    mechanics.forEach { mechanic ->
                        DropdownMenuItem(
                            text    = { Text(mechanic) },
                            onClick = {
                                selectedMechanic     = mechanic
                                mechanicDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Task list ─────────────────────────────────────────────────────
            if (form.assignedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(0.5.dp, Color.LightGray), RoundedCornerShape(10.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks added yet", fontSize = 12.sp, color = Color.LightGray)
                }
            } else {
                FieldLabel("Tasks added (${form.assignedTasks.size})")
                form.assignedTasks.forEach { task ->
                    AssignedTaskChip(
                        task     = task,
                        onRemove = { viewModel.removeTask(task.id) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Submit ────────────────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.submitCheckIn {
                        navController.popBackStack()
                    }
                },
                enabled  = viewModel.isFormValid(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor        = Color(0xFF1A1A1A),
                    disabledContainerColor = Color(0xFFCCCCCC)
                )
            ) {
                Text("Confirm Check-In", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Assigned task chip ───────────────────────────────────────────────────────

@Composable
fun AssignedTaskChip(
    task: AssignedTask,
    onRemove: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = task.description,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = "→ ${task.assignedTo}",
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick  = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Remove task",
                    tint               = Color.LightGray,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
fun FormSectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Medium,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier      = Modifier.padding(bottom = 8.dp)
    )
}

// ─── Reusable components ──────────────────────────────────────────────────────

@Composable
fun FieldLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier   = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ConditionSelector(
    selected: VehicleCondition,
    onSelect: (VehicleCondition) -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleCondition.entries.forEach { condition ->
                val isSelected = condition == selected
                Surface(
                    onClick = { onSelect(condition) },
                    shape   = RoundedCornerShape(20.dp),
                    color   = if (isSelected) Color(0xFF1A1A1A) else Color.Transparent,
                    border  = BorderStroke(
                        0.5.dp,
                        if (isSelected) Color(0xFF1A1A1A) else Color.LightGray
                    )
                ) {
                    Text(
                        text     = condition.label,
                        fontSize = 12.sp,
                        color    = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index < selected.filledSegments) Color(0xFF1A1A1A)
                            else Color(0xFFEEEEEE)
                        )
                )
            }
        }
    }
}

@Composable
fun PhotoPicker(
    uris: List<Uri>,
    onAddPhotos: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        uris.forEach { uri ->
            Image(
                painter            = rememberAsyncImagePainter(uri),
                contentDescription = "Vehicle photo",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
            )
        }
        if (uris.size < 5) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(BorderStroke(0.5.dp, Color.LightGray), RoundedCornerShape(8.dp))
                    .clickable { onAddPhotos() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add photo",
                    tint     = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun TruckCheckInPreview() {

}