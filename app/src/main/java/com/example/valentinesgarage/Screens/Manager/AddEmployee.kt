package com.example.valentinesgarage.Screens.Manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.PassWordHashing.PasswordUtils
import com.example.valentinesgarage.Screens.Manager.ViewFactory.AddEmployeeViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ─── Helper Functions ──────────────────────────────────────────────────────────

private fun buildNextEmployeeId(lastId: String?): String {
    if (lastId == null) return "EMP001"

    return try {
        val number = lastId.removePrefix("EMP").toInt()
        "EMP${(number + 1).toString().padStart(3, '0')}"
    } catch (e: Exception) {
        "EMP001"
    }
}

fun generatePassword(): String {
    val upper   = ('A'..'Z').toList()
    val lower   = ('a'..'z').toList()
    val digits  = ('0'..'9').toList()
    val special = listOf('!', '@', '#', '$', '%', '&')

    val required = listOf(
        upper.random(),
        upper.random(),
        lower.random(),
        lower.random(),
        digits.random(),
        digits.random(),
        special.random(),
        special.random(),
    )

    val allChars = upper + lower + digits + special
    val extra    = (1..(12 - required.size)).map { allChars.random() }

    return (required + extra).shuffled().joinToString("")
}

//-------------------- ViewModel --------------------------------------------------

class AddEmployeeViewModel(private val userDao: UserDao) : ViewModel() {

    var generatedEmployeeId by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val lastId = userDao.getLastEmployeeIdOnly()
            val nextId = buildNextEmployeeId(lastId)
            withContext(Dispatchers.Main) {
                generatedEmployeeId = nextId
            }
        }
    }

    fun addEmployee(
        employeeId:String,
        firstName:String,
        lastName:String,
        role: String,
        shift: String,
        email: String?,
        phone: String?,
        password: String,
        department: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch (Dispatchers.IO) {
            try {
                val hashedPassword = PasswordUtils.hashPassword(password)
                val joinDateString: String = LocalDate.now().toString()
                val employeeUser = com.example.valentinesgarage.Data.Entities.User(
                    employeeId    = employeeId,
                    firstName     = firstName,
                    lastName      = lastName,
                    role          = role.trim().lowercase(),
                    email         = email,
                    shift         = shift,
                    phone         = phone,
                    joinDate      = joinDateString,
                    taskCompleted = 0,
                    tasksPending  = 0,
                    taskProgress  = 0,
                    password      = hashedPassword,
                    department = department
                )
                userDao.insertUser(employeeUser)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
            }
        }
    }
}

// ─── Add Employee Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(navController: NavController, userDao: UserDao) {

    val viewModel: AddEmployeeViewModel = viewModel(
        factory = AddEmployeeViewModelFactory(userDao)
    )

    val clipboardManager = LocalClipboardManager.current

    // Form state
    var first_Name   by remember { mutableStateOf("") }
    var last_Name    by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var phone        by remember { mutableStateOf("") }
    var role         by remember { mutableStateOf("") }
    var department   by remember { mutableStateOf("") }
    var shift        by remember { mutableStateOf("") }

    var employeeId       by remember { mutableStateOf("Generating...") }
    var generatedPassword by remember { mutableStateOf(generatePassword()) }
    var passwordVisible   by remember { mutableStateOf(false) }
    var idCopied          by remember { mutableStateOf(false) }
    var pwCopied          by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.generatedEmployeeId) {
        if (viewModel.generatedEmployeeId.isNotEmpty()) {
            employeeId = viewModel.generatedEmployeeId
        }
    }

    var deptExpanded by remember { mutableStateOf(false) }
    val departments  = listOf("Engineering", "HR", "Maintenance", "Reception")

    val roleDrop = listOf("receptionist", "mechanic")
    var roleExpanded by remember { mutableStateOf(false) }

    // Validation errors
    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError  by remember { mutableStateOf(false) }
    var emailError     by remember { mutableStateOf(false) }
    var roleError      by remember { mutableStateOf(false) }

    var showSuccess by remember { mutableStateOf(false) }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {},
            icon  = { Icon(Icons.Default.Check, contentDescription = null) },
            title = { Text("Employee Added!") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$first_Name $last_Name has been successfully added.")
                    HorizontalDivider()
                    CredentialRow(label = "Employee ID", value = employeeId)
                    CredentialRow(label = "Password",    value = generatedPassword)
                    Text(
                        text  = "Please share these credentials securely with the employee.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Employee", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SectionHeader(title = "Personal Information", icon = Icons.Default.Person)

            FormField(
                value         = first_Name,
                onValueChange = { first_Name = it; firstNameError = false },
                label         = "First Name *",
                placeholder   = "e.g. Alice",
                isError       = firstNameError,
                errorMessage  = "First name is required",
                leadingIcon   = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            FormField(
                value         = last_Name,
                onValueChange = { last_Name = it; lastNameError = false },
                label         = "Last Name *",
                placeholder   = "e.g. Johnson",
                isError       = lastNameError,
                errorMessage  = "Last name is required",
                leadingIcon   = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            FormField(
                value         = email,
                onValueChange = { email = it; emailError = false },
                label         = "Email Address",
                placeholder   = "e.g. alice@company.com",
                isError       = emailError,
                errorMessage  = "Enter a valid email address",
                leadingIcon   = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            FormField(
                value           = phone,
                onValueChange   = { phone = it },
                label           = "Phone Number",
                placeholder     = "e.g. +264 81 000 0000",
                leadingIcon     = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            HorizontalDivider()

            SectionHeader(title = "Job Details", icon = Icons.Default.Work)

            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it }
            ) {
                OutlinedTextField(
                    value         = role,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Role *") },
                    isError       = roleError,
                    placeholder   = { Text("Select Role") },
                    leadingIcon   = { Icon(Icons.Default.Star, contentDescription = null) },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    shape         = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded         = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    roleDrop.forEach { roles ->
                        DropdownMenuItem(
                            text    = { Text(roles.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                role = roles
                                roleError = false
                                roleExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded         = deptExpanded,
                onExpandedChange = { deptExpanded = it }
            ) {
                OutlinedTextField(
                    value         = department,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Department") },
                    placeholder   = { Text("Select department") },
                    leadingIcon   = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    shape         = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded         = deptExpanded,
                    onDismissRequest = { deptExpanded = false }
                ) {
                    departments.forEach { dept ->
                        DropdownMenuItem(
                            text    = { Text(dept) },
                            onClick = {
                                department = dept
                                deptExpanded = false
                            }
                        )
                    }
                }
            }

            FormField(
                value           = shift,
                onValueChange   = { shift = it },
                label           = "Shift",
                placeholder     = "e.g. Day, Night",
                leadingIcon     = Icons.Default.Schedule,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            HorizontalDivider()

            SectionHeader(title = "Generated Credentials", icon = Icons.Default.Lock)

            OutlinedTextField(
                value         = employeeId,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Employee ID") },
                leadingIcon   = { Icon(Icons.Default.Badge, contentDescription = null) },
                trailingIcon  = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (employeeId =="Generating..."){
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(employeeId)); idCopied = true }) {
                            Icon(imageVector = if (idCopied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value               = generatedPassword,
                onValueChange       = {},
                readOnly            = true,
                label               = { Text("Generated Password") },
                leadingIcon         = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon        = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(generatedPassword)); pwCopied = true }) {
                            Icon(imageVector = if (pwCopied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            )

            // Password strength hint
            Surface(
                color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape  = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.Info,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(16.dp)
                    )
                    Text(
                        text  = "Password includes uppercase, lowercase, numbers and special characters. Hit refresh to regenerate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Button(
                onClick = {
                    firstNameError = first_Name.isBlank()
                    lastNameError  = last_Name.isBlank()
                    emailError     = email.isNotBlank() && !email.contains("@")
                    roleError      = role.isBlank()

                    if (!firstNameError && !lastNameError && !emailError && !roleError) {
                        viewModel.addEmployee(
                            employeeId, first_Name, last_Name, role, shift, email, phone, generatedPassword, department,
                            onSuccess = { showSuccess = true },
                            onError = { /* Handle error */ }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Employee", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FormField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String = "",
    isError: Boolean = false, errorMessage: String = "", leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
            label = { Text(label) }, placeholder = { Text(placeholder) }, isError = isError,
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            keyboardOptions = keyboardOptions, shape = RoundedCornerShape(12.dp), singleLine = true
        )
        if (isError) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}
