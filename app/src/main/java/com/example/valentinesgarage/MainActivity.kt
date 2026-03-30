package com.example.valentinesgarage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.AppNavigation.AppNavigation
import com.example.valentinesgarage.Login.Login
import com.example.valentinesgarage.Login.LoginViewModel
import com.example.valentinesgarage.Login.SignUpViewModel
import com.example.valentinesgarage.Screens.CheckIn.CheckInViewModel
import com.example.valentinesgarage.Screens.Mechanic.MechanicViewModel
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesViewModel
import kotlin.jvm.java


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mechanicViewModel = ViewModelProvider(this)[MechanicViewModel::class.java]
        val activeVehiclesViewModel = ViewModelProvider(this)[ActiveVehiclesViewModel::class.java]
        val loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        val checkInViewModel = ViewModelProvider(this)[CheckInViewModel::class.java]
        val signUpViewModel = ViewModelProvider(this)[SignUpViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            AppNavigation(mechanicViewModel, activeVehiclesViewModel, loginViewModel, checkInViewModel,
                signUpViewModel)
        }
    }
}
