package com.example.valentinesgarage.Login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Data.Entities.User
import com.example.valentinesgarage.PassWordHashing.PasswordUtils
import kotlinx.coroutines.launch

//---------------------------------- Login ViewModel
class LoginViewModel(private val userDao: UserDao) : ViewModel() {

    var loginResult by mutableStateOf<User?>(null)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    fun login(employeeId: String, password: String) {
        viewModelScope.launch {
            val user = userDao.getUserById(employeeId)

            if (user == null) {
                loginError = "User not found"
                loginResult = null
                return@launch
            }

            val passwordMatches = PasswordUtils.verifyPassword(password, user.password)

            if (passwordMatches) {
                loginResult = user
                loginError = null
            } else {
                loginResult = null
                loginError = "Incorrect password"
            }
        }
    }

    fun clearError() {
        loginError = null
    }
}

// ------------------------------- Login Screen
@Composable
fun Login(navController: NavController, userDao: UserDao) {

    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(userDao)
    )

    val user = viewModel.loginResult

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error popup
    LaunchedEffect(viewModel.loginError) {
        viewModel.loginError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Navigation based on role
    LaunchedEffect(user) {
        if (user != null) {
            when (user.role) {
                "manager" -> {
                    navController.navigate("ManagerHome") {
                        popUpTo("login") { inclusive = true }
                    }
                }

                "Mechanic", "employee" -> {
                    navController.navigate("EmployeePage") {
                        popUpTo("login") { inclusive = true }
                    }
                }

                else -> {
                    navController.navigate("EmployeePage")
                }
            }
        }
    }

    var employeeId by remember { mutableStateOf("") }
    var employeePassWord by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Login to your account",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Employee ID") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        focusedLabelColor = Color.Black
                    )
                )

                OutlinedTextField(
                    value = employeePassWord,
                    onValueChange = { employeePassWord = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        focusedLabelColor = Color.Black
                    ),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )
                Button(
                    onClick = {
                        viewModel.login(employeeId, employeePassWord)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Text(
                        text = "Login",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}