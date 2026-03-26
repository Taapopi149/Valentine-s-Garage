package com.example.valentinesgarage.AppNavigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.valentinesgarage.Login.Login
import com.example.valentinesgarage.Login.LoginViewModel
import com.example.valentinesgarage.Screens.CheckIn.CheckInViewModel
import com.example.valentinesgarage.Screens.CheckIn.TruckCheckInForm
import com.example.valentinesgarage.Screens.CheckIn.TruckCheckInScreen
import com.example.valentinesgarage.Screens.Employee.EmployeeHomePage
import com.example.valentinesgarage.Screens.Mechanic.MechanicTaskBoardScreen
import com.example.valentinesgarage.Screens.Mechanic.MechanicVehiclePickerScreen
import com.example.valentinesgarage.Screens.Mechanic.MechanicViewModel
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesScreen
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesViewModel


@Composable
fun AppNavigation(mechanicViewModel: MechanicViewModel, activeVehiclesViewModel: ActiveVehiclesViewModel, loginViewModel: LoginViewModel, checkInViewModel: CheckInViewModel) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "Login") {
        composable("Login") { Login(navController, loginViewModel) }
        composable("EmployeePage") { EmployeeHomePage(navController) }
        composable ("MechanicTaskList"){ MechanicVehiclePickerScreen(navController, mechanicViewModel) }
        composable ("ActiveVehicle"){ ActiveVehiclesScreen(navController, activeVehiclesViewModel) }
        composable ("TruckCheckIn"){ TruckCheckInScreen(navController,checkInViewModel ) }

        composable(
            route = "taskBoard/{jobId}"
        ) { backStackEntry ->

            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""

            MechanicTaskBoardScreen(
                navController = navController,
                jobId = jobId
            )
        }
    }

}