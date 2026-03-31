package com.example.valentinesgarage.Login

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
class LoginViewModel (private val userDao: UserDao): ViewModel() {
var loginResult by mutableStateOf<User?>(null)
    private set

var loginError by mutableStateOf<String?> (null)
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

}

// ------------------------------- Login Screen
@Composable
fun Login(navController: NavController, userDao: UserDao) {

    val viewModel: LoginViewModel = viewModel(
      factory = LoginViewModelFactory(userDao)
    )

    val user = viewModel.loginResult

    // Routing based on Role
    LaunchedEffect(user) {
        if (user != null) {
            when (user.role) {
                "manager" -> {
                    navController.navigate("ManagerHome") {
                        popUpTo("login") { inclusive = true }
                    }
                }

                "mechanic" -> {
                    navController.navigate("EmployeePage") {
                        popUpTo("login") { inclusive = true }
                    }
                }

                "employee" -> {
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

    val context = LocalContext.current
    var employeeId by remember { mutableStateOf("") }
    var employeePassWord by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center) {

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
                onValueChange = {employeeId = it},
                label = {Text("Employee ID")},
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    focusedLabelColor = Color.Black
                )

            )

            Spacer(modifier = Modifier.height(5.dp))

            OutlinedTextField(
                value = employeePassWord,
                onValueChange = { employeePassWord = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    focusedLabelColor = Color.Black
                )


            )

            Button(
                onClick = {
                    viewModel.login(employeeId, employeePassWord)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A)

                )

            ) {

                Text(text = "Login",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                    )
            }

            LaunchedEffect(viewModel.loginResult) {
                viewModel.loginResult?.let {
                    user -> when(user.role) {
                        "manager" -> navController.navigate("ManagerHome")
                        "mechanic" -> navController.navigate("EmployeePage")
                    else -> println("Unknown Role")
                    }
                }
            }


        }

    }



}