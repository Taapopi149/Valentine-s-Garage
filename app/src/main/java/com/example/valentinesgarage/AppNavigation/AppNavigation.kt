package com.example.valentinesgarage.AppNavigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Login.Login
import com.example.valentinesgarage.Login.LoginViewModel
import com.example.valentinesgarage.Login.SignUp
import com.example.valentinesgarage.Manager.ManagerDashboardScreen
import com.example.valentinesgarage.Screens.CheckIn.CheckInViewModel
import com.example.valentinesgarage.Screens.CheckIn.TruckCheckInScreen
import com.example.valentinesgarage.Screens.Employee.EmployeeHomePage
import com.example.valentinesgarage.Screens.Employee.EmployeeProfilePage
import com.example.valentinesgarage.Screens.Manager.AddEmployeeScreen
import com.example.valentinesgarage.Screens.Mechanic.MechanicTaskBoardScreen
import com.example.valentinesgarage.Screens.Mechanic.MechanicVehiclePickerScreen
import com.example.valentinesgarage.Screens.Mechanic.MechanicViewModel
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesScreen
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesViewModel
import com.example.valentinesgarage.Screens.Vehicles.TruckDetailScreen

/**
 * Root navigation component of the application.
 * Defines all routes and handles transitions between screens.
 */
@Composable
fun AppNavigation(
    mechanicViewModel: MechanicViewModel,
    activeVehiclesViewModel: ActiveVehiclesViewModel,
    checkInViewModel: CheckInViewModel,
    userDao: UserDao
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "SignUp") {

        // Authentication Flow
        composable("SignUp") {
            SignUp(navController, userDao)
        }

        composable("Login") {
            Login(navController, userDao)
        }

        // Employee & Receptionist Dashboard
        composable("EmployeePage") { 
            EmployeeHomePage(navController) 
        }

        // Service & Maintenance Flow
        composable("MechanicTaskList") { 
            MechanicVehiclePickerScreen(navController, mechanicViewModel) 
        }
        
        composable("ActiveVehicle") { 
            ActiveVehiclesScreen(navController, activeVehiclesViewModel) 
        }
        
        composable("TruckCheckIn") { 
            TruckCheckInScreen(navController, checkInViewModel) 
        }

        // Manager Flow
        composable("ManagerHome") {
            ManagerDashboardScreen(navController, userDao) 
        }

        composable("addEmployee") {
            AddEmployeeScreen(navController, userDao)
        }

        // Detailed View Routes
        composable(route = "taskBoard/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            MechanicTaskBoardScreen(navController = navController, jobId = jobId)
        }

        composable(
            route = "truckDetail/{truckId}",
            arguments = listOf(navArgument("truckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val truckId = backStackEntry.arguments?.getString("truckId")
            TruckDetailScreen(navController = navController, truckId = truckId)
        }

        composable("Profile") { 
            EmployeeProfilePage(navController) 
        }
    }
}
