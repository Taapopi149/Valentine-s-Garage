package com.example.valentinesgarage.Screens.Employee

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Data.Entities.User
import com.example.valentinesgarage.Screens.Employee.ViewModelFactory.EmployeeProfileViewModelFactory
import com.example.valentinesgarage.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

// ─── UI model ─────────────────────────────────────────────────────────────────

data class EmployeeProfile(
    val name:            String,
    val role:            String,
    val employeeId:      String,
    val email:           String,
    val phone:           String,
    val department:      String,
    val shift:           String,
    val joinDate:        String,
    val tasksCompleted:  Int,
    val tasksInProgress: Int,
    val tasksPending:    Int
)

// ─── Colour palette ───────────────────────────────────────────────────────────

private val DarkInk      = Color(0xFF111318)
private val DividerGray  = Color(0xFFE4E4E7)
private val AccentOrange = Color(0xFFE8500A)
private val SubtleGray   = Color(0xFF8A8A96)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class EmployeeProfileViewModel(
    private val userDao:    UserDao,
    private val tasksDao:   TasksDao,
    private val employeeId: String
) : ViewModel() {

    // ── Profile state ─────────────────────────────────────────────────────────

    private val _profile   = MutableStateFlow<EmployeeProfile?>(null)
    val profile: StateFlow<EmployeeProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Edit form state ───────────────────────────────────────────────────────

    data class EditFormState(
        val firstName:  String = "",
        val lastName:   String = "",
        val email:      String = "",
        val phone:      String = "",
        val department: String = "",
        val shift:      String = ""
    )

    private val _editForm  = MutableStateFlow(EditFormState())
    val editForm: StateFlow<EditFormState> = _editForm.asStateFlow()

    private val _isSaving  = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userDao.getUserId(employeeId)
                .filterNotNull()
                .collect { user ->
                    val completed  = tasksDao.getTaskCountByStatus(employeeId, "Done")
                    val inProgress = tasksDao.getTaskCountByStatus(employeeId, "In Progress")
                    val pending    = tasksDao.getTaskCountByStatus(employeeId, "Pending")

                    _profile.value = user.toProfile(
                        tasksCompleted  = completed,
                        tasksInProgress = inProgress,
                        tasksPending    = pending
                    )
                    _isLoading.value = false
                }
        }
    }

    // ── Edit helpers ──────────────────────────────────────────────────────────

    // Pre-populate form from current profile so fields aren't blank when sheet opens
    fun initEditForm() {
        val p = _profile.value ?: return
        val nameParts = p.name.split(" ", limit = 2)
        _editForm.value = EditFormState(
            firstName  = nameParts.getOrElse(0) { "" },
            lastName   = nameParts.getOrElse(1) { "" },
            email      = if (p.email      == "—") "" else p.email,
            phone      = if (p.phone      == "—") "" else p.phone,
            department = if (p.department == "—") "" else p.department,
            shift      = if (p.shift      == "—") "" else p.shift
        )
    }

    fun onFirstNameChange(v: String)  { _editForm.value = _editForm.value.copy(firstName  = v) }
    fun onLastNameChange(v: String)   { _editForm.value = _editForm.value.copy(lastName   = v) }
    fun onEmailChange(v: String)      { _editForm.value = _editForm.value.copy(email      = v) }
    fun onPhoneChange(v: String)      { _editForm.value = _editForm.value.copy(phone      = v) }
    fun onDepartmentChange(v: String) { _editForm.value = _editForm.value.copy(department = v) }
    fun onShiftChange(v: String)      { _editForm.value = _editForm.value.copy(shift      = v) }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveProfile(onDone: () -> Unit) {
        val form = _editForm.value
        if (form.firstName.isBlank() || form.lastName.isBlank()) {
            _saveError.value = "First and last name are required."
            return
        }
        viewModelScope.launch {
            _isSaving.value  = true
            _saveError.value = null
            try {
                userDao.updateUser(
                    employeeId  = employeeId,
                    firstName   = form.firstName.trim(),
                    lastName    = form.lastName.trim(),
                    email       = form.email.trim(),
                    phone       = form.phone.trim(),
                    department  = form.department.trim(),
                    shift       = form.shift.trim()
                )
                // Profile reloads automatically via the Flow collector above
                onDone()
            } catch (e: Exception) {
                _saveError.value = "Failed to save: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

// ─── Mapper ───────────────────────────────────────────────────────────────────

private fun User.toProfile(
    tasksCompleted:  Int,
    tasksInProgress: Int,
    tasksPending:    Int
) = EmployeeProfile(
    name            = "$firstName $lastName",
    role            = role,
    employeeId      = employeeId,
    email           = email      ?: "—",
    phone           = phone      ?: "—",
    department      = department ?: "—",
    shift           = shift      ?: "—",
    joinDate        = joinDate   ?: "—",
    tasksCompleted  = tasksCompleted,
    tasksInProgress = tasksInProgress,
    tasksPending    = tasksPending
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeProfilePage(
    navController: NavController,
    userDao:       UserDao,
    tasksDao:      TasksDao
) {
    val employeeId = SessionManager.currentUserId ?: ""

    val viewModel: EmployeeProfileViewModel = viewModel(
        factory = EmployeeProfileViewModelFactory(userDao, tasksDao, employeeId)
    )

    val profile       by viewModel.profile.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val editForm      by viewModel.editForm.collectAsState()
    val isSaving      by viewModel.isSaving.collectAsState()
    val saveError     by viewModel.saveError.collectAsState()
    var showEditSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF2F2F4),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint               = DarkInk
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.initEditForm()   // pre-fill fields before opening
                        showEditSheet = true
                    }) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint               = DarkInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->

        // ── Loading ───────────────────────────────────────────────────────────
        if (isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentOrange)
            }
            return@Scaffold
        }

        // ── No user (shouldn't happen) ────────────────────────────────────────
        if (profile == null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Profile not found", color = SubtleGray)
            }
            return@Scaffold
        }

        val employee = profile!!

        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding      = PaddingValues(bottom = 32.dp)
        ) {

            // ── Hero Header ───────────────────────────────────────────────────
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(AccentOrange.copy(alpha = 0.15f))
                                .border(2.dp, AccentOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = employee.name
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString(""),
                                fontSize   = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.Black
                            )
                        }

                        Text(
                            text       = employee.name,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.Black
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AccentOrange.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text       = employee.role,
                                color      = AccentOrange,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text          = employee.employeeId,
                            fontSize      = 12.sp,
                            color         = Color.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // ── Stats Row ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(Modifier.weight(1f), employee.tasksCompleted.toString(),  "Done",    Color(0xFF2E7D32))
                    StatChip(Modifier.weight(1f), employee.tasksInProgress.toString(), "Active",  Color(0xFF1565C0))
                    StatChip(Modifier.weight(1f), employee.tasksPending.toString(),    "Pending", Color(0xFFE65100))
                }
            }

            // ── Contact Info ──────────────────────────────────────────────────
            item {
                ProfileSection(title = "Contact Information") {
                    InfoRow(Icons.Default.Email, "Email", employee.email)
                    SectionDivider()
                    InfoRow(Icons.Default.Phone, "Phone", employee.phone)
                }
            }

            // ── Work Details ──────────────────────────────────────────────────
            item {
                ProfileSection(title = "Work Details") {
                    InfoRow(Icons.Default.Build,     "Department", employee.department)
                    SectionDivider()
                    InfoRow(Icons.Default.Schedule,  "Shift",      employee.shift)
                    SectionDivider()
                    InfoRow(Icons.Default.DateRange, "Joined",     employee.joinDate)
                }
            }

            // ── Sign Out ──────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick  = {
                        SessionManager.currentUserId = null
                        navController.navigate("Login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    shape  = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFD32F2F), Color(0xFFD32F2F)))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        // ── Edit bottom sheet ─────────────────────────────────────────────────
        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                EditProfileSheet(
                    form      = editForm,
                    isSaving  = isSaving,
                    saveError = saveError,
                    viewModel = viewModel,
                    onDismiss = { showEditSheet = false }
                )
            }
        }
    }
}

// ─── Edit bottom sheet ────────────────────────────────────────────────────────

@Composable
private fun EditProfileSheet(
    form:      EmployeeProfileViewModel.EditFormState,
    isSaving:  Boolean,
    saveError: String?,
    viewModel: EmployeeProfileViewModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = "Edit Profile",
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color      = DarkInk,
            modifier   = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        HorizontalDivider(color = DividerGray)

        // Name row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value         = form.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label         = { Text("First name") },
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value         = form.lastName,
                onValueChange = viewModel::onLastNameChange,
                label         = { Text("Last name") },
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(10.dp)
            )
        }

        OutlinedTextField(
            value         = form.email,
            onValueChange = viewModel::onEmailChange,
            label         = { Text("Email") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            leadingIcon   = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) }
        )

        OutlinedTextField(
            value         = form.phone,
            onValueChange = viewModel::onPhoneChange,
            label         = { Text("Phone") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            leadingIcon   = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) }
        )

        OutlinedTextField(
            value         = form.department,
            onValueChange = viewModel::onDepartmentChange,
            label         = { Text("Department") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            leadingIcon   = { Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp)) }
        )

        OutlinedTextField(
            value         = form.shift,
            onValueChange = viewModel::onShiftChange,
            label         = { Text("Shift") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            leadingIcon   = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp)) }
        )

        // Error message
        if (saveError != null) {
            Text(
                text     = saveError,
                color    = Color(0xFFD32F2F),
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        // Save button
        Button(
            onClick  = { viewModel.saveProfile(onDone = { onDismiss() }) },
            enabled  = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = DarkInk)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color       = Color.White,
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save changes", fontWeight = FontWeight.SemiBold)
            }
        }

        // Cancel button
        OutlinedButton(
            onClick  = onDismiss,
            enabled  = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text("Cancel")
        }
    }
}

// ─── Reusable components ──────────────────────────────────────────────────────

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    value:    String,
    label:    String,
    color:    Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                text       = label,
                fontSize   = 11.sp,
                color      = SubtleGray,
                fontWeight = FontWeight.Medium,
                textAlign  = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileSection(
    title:   String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier            = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text          = title.uppercase(),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = SubtleGray,
            letterSpacing = 1.2.sp,
            modifier      = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun InfoRow(
    icon:  ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFF0F0F2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = DarkInk,
                modifier           = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = SubtleGray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkInk)
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 66.dp, end = 16.dp),
        color     = DividerGray,
        thickness = 0.8.dp
    )
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun EmployeeProfilePreview() {
    MaterialTheme {
        val fake = EmployeeProfile(
            name            = "John Mutanga",
            role            = "Senior Mechanic",
            employeeId      = "EMP-00142",
            email           = "j.mutanga@valentinesgarage.com",
            phone           = "+264 81 234 5678",
            department      = "Engine & Drivetrain",
            shift           = "Morning  |  06:00 – 14:00",
            joinDate        = "12 March 2021",
            tasksCompleted  = 128,
            tasksInProgress = 3,
            tasksPending    = 5
        )
        Scaffold(containerColor = Color(0xFFF2F2F4)) { padding ->
            LazyColumn(
                modifier            = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding      = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(AccentOrange.copy(alpha = 0.15f))
                                    .border(2.dp, AccentOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("JM", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Text(fake.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Surface(shape = RoundedCornerShape(20.dp), color = AccentOrange.copy(alpha = 0.18f)) {
                                Text(
                                    text       = fake.role,
                                    color      = AccentOrange,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }
                            Text(fake.employeeId, fontSize = 12.sp, color = Color.Black, letterSpacing = 1.sp)
                        }
                    }
                }
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip(Modifier.weight(1f), fake.tasksCompleted.toString(),  "Done",    Color(0xFF2E7D32))
                        StatChip(Modifier.weight(1f), fake.tasksInProgress.toString(), "Active",  Color(0xFF1565C0))
                        StatChip(Modifier.weight(1f), fake.tasksPending.toString(),    "Pending", Color(0xFFE65100))
                    }
                }
                item {
                    ProfileSection("Contact Information") {
                        InfoRow(Icons.Default.Email, "Email", fake.email)
                        SectionDivider()
                        InfoRow(Icons.Default.Phone, "Phone", fake.phone)
                    }
                }
                item {
                    ProfileSection("Work Details") {
                        InfoRow(Icons.Default.Build,     "Department", fake.department)
                        SectionDivider()
                        InfoRow(Icons.Default.Schedule,  "Shift",      fake.shift)
                        SectionDivider()
                        InfoRow(Icons.Default.DateRange, "Joined",     fake.joinDate)
                    }
                }
            }
        }
    }
}