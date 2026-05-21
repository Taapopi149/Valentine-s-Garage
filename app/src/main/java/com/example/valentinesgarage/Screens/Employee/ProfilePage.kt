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

    private val _profile   = MutableStateFlow<EmployeeProfile?>(null)
    val profile: StateFlow<EmployeeProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
                onDone()
            } catch (e: Exception) {
                _saveError.value = "Failed to save: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

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
                        viewModel.initEditForm()
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

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
            return@Scaffold
        }

        if (profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
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
            item {
                Box(Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 28.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                        Text(employee.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                        Surface(shape = RoundedCornerShape(20.dp), color = AccentOrange.copy(alpha = 0.18f)) {
                            Text(
                                text = employee.role,
                                color = AccentOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Text(employee.employeeId, fontSize = 12.sp, color = Color.Black, letterSpacing = 1.sp)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.spacedBy(10.dp)) {
                    StatChip(Modifier.weight(1f), employee.tasksCompleted.toString(),  "Done",    Color(0xFF2E7D32))
                    StatChip(Modifier.weight(1f), employee.tasksInProgress.toString(), "Active",  Color(0xFF1565C0))
                    StatChip(Modifier.weight(1f), employee.tasksPending.toString(),    "Pending", Color(0xFFE65100))
                }
            }

            item {
                ProfileSection(title = "Contact Information") {
                    InfoRow(Icons.Default.Email, "Email", employee.email)
                    SectionDivider()
                    InfoRow(Icons.Default.Phone, "Phone", employee.phone)
                }
            }

            item {
                ProfileSection(title = "Work Details") {
                    InfoRow(Icons.Default.Build,     "Department", employee.department)
                    SectionDivider()
                    InfoRow(Icons.Default.Schedule,  "Shift",      employee.shift)
                    SectionDivider()
                    InfoRow(Icons.Default.DateRange, "Joined",     employee.joinDate)
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick  = {
                        SessionManager.currentUserId = null
                        navController.navigate("Login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    shape  = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFD32F2F), Color(0xFFD32F2F)))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                EditProfileSheet(editForm, isSaving, saveError, viewModel, onDismiss = { showEditSheet = false })
            }
        }
    }
}

@Composable
private fun EditProfileSheet(
    form:      EmployeeProfileViewModel.EditFormState,
    isSaving:  Boolean,
    saveError: String?,
    viewModel: EmployeeProfileViewModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Edit Profile", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DarkInk, modifier = Modifier.padding(vertical = 4.dp))
        HorizontalDivider(color = DividerGray)

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(form.firstName, viewModel::onFirstNameChange, label = { Text("First name") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true)
            OutlinedTextField(form.lastName, viewModel::onLastNameChange, label = { Text("Last name") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true)
        }

        OutlinedTextField(form.email, viewModel::onEmailChange, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) })
        OutlinedTextField(form.phone, viewModel::onPhoneChange, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) })
        OutlinedTextField(form.department, viewModel::onDepartmentChange, label = { Text("Department") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp)) })
        OutlinedTextField(form.shift, viewModel::onShiftChange, label = { Text("Shift") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp)) })

        if (saveError != null) {
            Text(saveError, color = Color(0xFFD32F2F), fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick  = { viewModel.saveProfile(onDone = onDismiss) },
            enabled  = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = DarkInk)
        ) {
            if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Save changes", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(onDismiss, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Cancel")
        }
    }
}

@Composable
private fun StatChip(modifier: Modifier = Modifier, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = SubtleGray, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}
@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), Arrangement.spacedBy(0.dp)) {
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SubtleGray, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFF0F0F2)), Alignment.Center) {
            Icon(icon, null, tint = DarkInk, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = SubtleGray, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(1.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkInk)
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(Modifier.padding(start = 66.dp, end = 16.dp), color = DividerGray, thickness = 0.8.dp)
}
