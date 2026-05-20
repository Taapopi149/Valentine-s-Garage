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

//fun generateEmployeeId(): String {
//    val number = Random.nextInt(1000, 9999)
//    return "EMP-$number"
//}


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
    val letters = ('a'..'z')
    val numbers = ('0'..'9')
    val allChars = letters + numbers

    return (1..6)
        .map { allChars.random() }
        .joinToString("")
}
//-------------------- ViewModel --------------------------------------------------

class AddEmployeeViewModel(private val userDao: UserDao) : ViewModel() {


    var generatedEmployeeId by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val lastId = userDao.getLastEmployeeIdOnly()   // e.g. "MG003" or null
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

        val hashedPassword = PasswordUtils.hashPassword(password)


        viewModelScope.launch (Dispatchers.IO) {
            val joinDateString: String = LocalDate.now().toString()
            val employeeUser = com.example.valentinesgarage.Data.Entities.User(
                employeeId    = employeeId,
                firstName     = firstName,
                lastName      = lastName,
                role          = role,
                email         = email,
                shift         = shift,
                phone         = phone,
                joinDate      = joinDateString,
                taskCompleted = null,
                tasksPending  = null,
                taskProgress  = null,
                password      = hashedPassword,
                department = department
            )
            userDao.insertUser(employeeUser)
            withContext(Dispatchers.Main) {onSuccess()}
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
    var last_Name   by remember { mutableStateOf("") }
    var email      by remember { mutableStateOf("") }
    var phone      by remember { mutableStateOf("") }
    var role       by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
     var shift     by remember { mutableStateOf("") }



    // Generated credentials — created once when the screen opens
    var employeeId       by remember { mutableStateOf("Generating...") }
    var generatedPassword by remember { mutableStateOf(generatePassword()) }
    var passwordVisible  by remember { mutableStateOf(false) }
    var idCopied         by remember { mutableStateOf(false) }
    var pwCopied         by remember { mutableStateOf(false) }

    // Once the ViewModel finishes the DB lookup, sync the ID into local state

    LaunchedEffect(viewModel.generatedEmployeeId) {
        if (viewModel.generatedEmployeeId.isNotEmpty()) {
            employeeId = viewModel.generatedEmployeeId
        }
    }

    // Dropdown state
    var deptExpanded by remember { mutableStateOf(false) }
    val departments  = listOf("Engineering", "HR")

    // Role
    val roleDrop = listOf("receptionist ", "mechanic")
    var roleExpanded by remember { mutableStateOf(false) }

    // Validation errors
    var nameError  by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var roleError  by remember { mutableStateOf(false) }

    // Success dialog
    var showSuccess by remember { mutableStateOf(false) }

    // ── Success Dialog ──────────────────────────────────────────────────────
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {},
            icon  = { Icon(Icons.Default.Check, contentDescription = null) },
            title = { Text("Employee Added!") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${first_Name} ${last_Name}  has been successfully added.")
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

    // ── Scaffold ────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Add Employee",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

            // ── Section: Personal Info ──────────────────────────────────────
            SectionHeader(title = "Personal Information", icon = Icons.Default.Person)

            FormField(
                value         = first_Name,
                onValueChange = { first_Name = it; nameError = false },
                label         = "First Name *",
                placeholder   = "e.g. Alice",
                isError       = nameError,
                errorMessage  = "Full name is required",
                leadingIcon   = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            FormField(
                value         = last_Name,
                onValueChange = { last_Name = it; nameError = false },
                label         = "Last Name",
                placeholder   = "e.g. Johnson",
                isError       = nameError,
                errorMessage  = "Full name is required",
                leadingIcon   = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )


            FormField(
                value         = email,
                onValueChange = { email = it; emailError = false },
                label         = "Email Address ",
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

            // ── Section: Job Details ────────────────────────────────────────
            SectionHeader(title = "Job Details", icon = Icons.Default.Work)



            // Role DropDown
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = {roleExpanded = it}
            ) {
               OutlinedTextField(
                   value         = role,
                   onValueChange = {},
                   readOnly      = true,
                   label         = { Text("Role") },
                   placeholder   = { Text("Select Role") },
                   leadingIcon   = {
                       Icon(Icons.Default.Star, contentDescription = null)
                   },
                   trailingIcon  = {
                       ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded)
                   },
                   modifier      = Modifier
                       .fillMaxWidth()
                       .menuAnchor(),
                   shape         = RoundedCornerShape(12.dp)
               )
                ExposedDropdownMenu(
                    expanded         = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    roleDrop.forEach { roles ->
                        DropdownMenuItem(
                            text    = { Text(roles) },
                            onClick = {
                                role   = roles
                                roleExpanded = false
                            }
                        )
                    }
                }
            }



            // Department Dropdown
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
                    leadingIcon   = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    trailingIcon  = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded)
                    },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
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
                                department   = dept
                                deptExpanded = false
                            }
                        )
                    }
                }
            }

            FormField(
                value           = shift,
                onValueChange   = { shift = it },
                label           = "shift",
                placeholder     = "Night, Day",
                leadingIcon     = Icons.Default.List,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            HorizontalDivider()

            // ── Section: Generated Credentials ─────────────────────────────
            SectionHeader(title = "Generated Credentials", icon = Icons.Default.Lock)

            // Employee ID field
            OutlinedTextField(
                value         = employeeId,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Employee ID") },
                leadingIcon   = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                trailingIcon  = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Regenerate ID button

                        if (employeeId =="Generating..."){
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }


                        IconButton(onClick = {
                            idCopied   = false
                        }) {
                            Icon(
                                imageVector        = Icons.Default.Refresh,
                                contentDescription = "Regenerate ID",
                                tint               = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Copy ID button
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(employeeId))
                            idCopied = true
                        }) {
                            Icon(
                                imageVector        = if (idCopied) Icons.Default.Check else Icons.Default.Share,
                                contentDescription = "Copy ID",
                                tint               = if (idCopied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = OutlinedTextFieldDefaults.colors(
                    disabledTextColor        = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor      = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor       = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
            )

            // Generated Password field
            OutlinedTextField(
                value               = generatedPassword,
                onValueChange       = {},
                readOnly            = true,
                label               = { Text("Generated Password") },
                leadingIcon         = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon        = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector        = if (passwordVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                        // Regenerate password
                        IconButton(onClick = {
                            generatedPassword = generatePassword()
                            pwCopied          = false
                        }) {
                            Icon(
                                imageVector        = Icons.Default.Refresh,
                                contentDescription = "Regenerate password",
                                tint               = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Copy password
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(generatedPassword))
                            pwCopied = true
                        }) {
                            Icon(
                                imageVector        = if (pwCopied) Icons.Default.Check else Icons.Default.Share,
                                contentDescription = "Copy password",
                                tint               = if (pwCopied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
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

            // ── Action Buttons ──────────────────────────────────────────────
            Button(
                onClick = {
                    // Validate
                    nameError  = first_Name.isBlank()
                    nameError = last_Name.isBlank()
                    emailError = email.isBlank() || !email.contains("@")
                    roleError  = role.isBlank()

                    if (!nameError && !emailError && !roleError) {
                        viewModel.addEmployee(
                            employeeId,
                            first_Name,
                            last_Name,
                            role,
                            shift,
                            email,
                            phone,
                            generatedPassword,
                            department,
                            onSuccess = {
                                showSuccess = true
                            },
                            onError = {
                                println("Error adding employee: \$errorMsg")
                            }
                        )
                    }

                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Add Employee",
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp)
        )
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}
@Composable
private fun FormField(
    value:           String,
    onValueChange:   (String) -> Unit,
    label:           String,
    placeholder:     String          = "",
    isError:         Boolean         = false,
    errorMessage:    String          = "",
    leadingIcon:     ImageVector?    = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value           = value,
            onValueChange   = onValueChange,
            modifier        = Modifier.fillMaxWidth(),
            label           = { Text(label) },
            placeholder     = { Text(placeholder) },
            isError         = isError,
            leadingIcon     = leadingIcon?.let {
                { Icon(it, contentDescription = null) }
            },
            keyboardOptions = keyboardOptions,
            shape           = RoundedCornerShape(12.dp),
            singleLine      = true
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text  = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}