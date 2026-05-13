package com.example.valentinesgarage.Login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.PassWordHashing.PasswordUtils
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valentinesgarage.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── ViewModel ────────────────────────────────────────────────────────────────

class SignUpViewModel(private val userDao: UserDao) : ViewModel() {

    // UI observes this  starts empty, fills once DB lookup completes
    var generatedEmployeeId by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val lastId = userDao.getLastEmployeeId()   // e.g. "MG003" or null
            val nextId = buildNextEmployeeId(lastId)
            withContext(Dispatchers.Main) {
                generatedEmployeeId = nextId
            }
        }
    }

    // ─── ID Generation ────────────────────────────────────────────────────────
    private fun buildNextEmployeeId(lastId: String?): String {
        if (lastId == null) return "MG001"             // very first manager
        return try {
            val number = lastId.removePrefix("MG").toInt()
            "MG${(number + 1).toString().padStart(3, '0')}"  // MG001 → MG002 … MG099 → MG100
        } catch (e: NumberFormatException) {
            "MG001"                                    // fallback if DB value is malformed
        }
    }

    // ─── Sign Up ──────────────────────────────────────────────────────────────
    fun signUpManager(
        employeeId: String,
        firstName:  String,
        lastName:   String,
        email:      String?,
        phone:      String?,
        password:   String,
        onSuccess:  () -> Unit,
        onError:    (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hashedPassword = PasswordUtils.hashPassword(password)
                val managerUser = com.example.valentinesgarage.Data.Entities.User(
                    employeeId    = employeeId,
                    firstName     = firstName,
                    lastName      = lastName,
                    role          = "manager",
                    email         = email,
                    shift         = null,
                    phone         = phone,
                    joinDate      = null,
                    taskCompleted = null,
                    tasksPending  = null,
                    taskProgress  = null,
                    password      = hashedPassword,
                    department = null
                )
                userDao.insertUser(managerUser)


                //Save User State
                SessionManager.login(managerUser)

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to create manager account")
                }
            }
        }
    }
}

// ─── Sign Up Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUp(navController: NavController, userDao: UserDao) {

    val viewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModelFactory(userDao)
    )

    // Form state
    var firstName       by remember { mutableStateOf("") }
    var lastName        by remember { mutableStateOf("") }
    var employeeId      by remember { mutableStateOf("Generating…") }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMismatch by remember { mutableStateOf(false) }

    // Once the ViewModel finishes the DB lookup, sync the ID into local state
    LaunchedEffect(viewModel.generatedEmployeeId) {
        if (viewModel.generatedEmployeeId.isNotEmpty()) {
            employeeId = viewModel.generatedEmployeeId
        }
    }

    val isFormValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            employeeId.isNotBlank() &&
            employeeId != "Generating…" &&   // don't allow submit before ID is ready
            email.isNotBlank() &&
            phone.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            password == confirmPassword

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text       = "Create Employee Account",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
                Text(
                    text      = "Fill in the details below to register",
                    fontSize  = 13.sp,
                    color     = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                focusedLabelColor  = Color.Black
            )
            val fieldShape = RoundedCornerShape(10.dp)

            OutlinedTextField(
                value         = firstName,
                onValueChange = { firstName = it },
                label         = { Text("First Name") },
                shape         = fieldShape,
                colors        = fieldColors,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = lastName,
                onValueChange = { lastName = it },
                label         = { Text("Last Name") },
                shape         = fieldShape,
                colors        = fieldColors,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // Employee ID — read-only, auto-filled from DB
            OutlinedTextField(
                value         = employeeId,
                onValueChange = {},
                label         = { Text("Employee ID") },
                shape         = fieldShape,
                colors        = fieldColors,
                singleLine    = true,
                readOnly      = true,
                modifier      = Modifier.fillMaxWidth(),
                // Show a spinner inside the field while the ID is being generated
                trailingIcon  = {
                    if (employeeId == "Generating…") {
                        CircularProgressIndicator(
                            modifier  = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color     = Color.Black
                        )
                    }
                }
            )

            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                label         = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape         = fieldShape,
                colors        = fieldColors,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value           = phone,
                onValueChange   = { phone = it },
                label           = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape           = fieldShape,
                colors          = fieldColors,
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = password,
                onValueChange = {
                    password = it
                    passwordMismatch = false
                },
                label                = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape                = fieldShape,
                colors               = fieldColors,
                singleLine           = true,
                modifier             = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordMismatch = false
                },
                label                = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError              = passwordMismatch,
                supportingText       = {
                    if (passwordMismatch) Text(
                        "Passwords do not match",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                shape      = fieldShape,
                colors     = fieldColors,
                singleLine = true,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        passwordMismatch = true
                        return@Button
                    }
                    viewModel.signUpManager(
                        employeeId = employeeId,
                        firstName  = firstName,
                        lastName   = lastName,
                        email      = email,
                        phone      = phone,
                        password   = password,
                        onSuccess  = { navController.navigate("ManagerHome") },
                        onError    = { message -> println("Error: $message") }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape   = RoundedCornerShape(12.dp),
                enabled = isFormValid,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text(
                    text       = "Create Account",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = "Already have an account? ",
                    fontSize = 13.sp,
                    color    = Color(0xFF888888)
                )
                TextButton(
                    onClick        = { navController.navigate("Login") },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text       = "Log in",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF1A1A1A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}