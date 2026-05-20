package com.example.valentinesgarage.AppNavigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.valentinesgarage.Data.DAO.NotesDao
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Login.Login
import com.example.valentinesgarage.Login.SignUp
import com.example.valentinesgarage.Manager.ManagerDashboardScreen
import com.example.valentinesgarage.Screens.CheckIn.TruckCheckInScreen
import com.example.valentinesgarage.Screens.Employee.EmployeeHomePage
import com.example.valentinesgarage.Screens.Employee.EmployeeProfilePage
import com.example.valentinesgarage.Screens.Manager.AddEmployeeScreen
import com.example.valentinesgarage.Screens.Manager.ReportsScreen
import com.example.valentinesgarage.Screens.Mechanic.MechanicTaskBoardScreen
import com.example.valentinesgarage.Screens.Mechanic.MechanicVehiclePickerScreen
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesScreen
import com.example.valentinesgarage.Screens.Vehicles.TruckDetailScreen
import com.example.valentinesgarage.SessionManager



@Composable
fun AppNavigation(
                  userDao: UserDao,
                  truckDao: TruckDao,
                  noteDao: NotesDao,
                  taskDao: TasksDao
                  ) {

    val navController = rememberNavController()



    NavHost(navController = navController, startDestination = "SignUp") {

        composable("SignUp") {
            SignUp(navController, userDao)
        }

        composable("Login") {
            Login(navController, userDao) }

       // composable("reports") {ReportsScreen(navController)}

        composable("EmployeePage") { EmployeeHomePage(navController, truckDao, taskDao) }
        composable ("MechanicTaskList"){ MechanicVehiclePickerScreen(navController, truckDao, taskDao, userDao) }
        composable ("ActiveVehicle"){ ActiveVehiclesScreen(navController, truckDao) }
        composable ("TruckCheckIn"){ TruckCheckInScreen(navController,truckDao, noteDao, taskDao, userDao) }
        composable ("ManagerHome"){ManagerDashboardScreen(navController, userDao, truckDao, taskDao) }

        composable("addEmployee"){AddEmployeeScreen(navController, userDao)}

        composable(
            route = "taskBoard/{jobId}"
        ) { backStackEntry ->

            val jobId = backStackEntry.arguments?.getString("jobId")?.toIntOrNull() ?: return@composable
            MechanicTaskBoardScreen(
                navController = navController,
                jobId = jobId,
                currentMechanic = SessionManager.currentUser?.firstName ?: "Mechanic",
                truckDao = truckDao,
                tasksDao = taskDao,
                userDao = userDao
            )
        }

        composable(
            route = "truckDetail/{truckId}",
            arguments = listOf(
                navArgument("truckId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val truckId = backStackEntry.arguments?.getString("truckId")?.toIntOrNull() ?: return@composable
            TruckDetailScreen(
                navController = navController,
                truckId = truckId,
                truckDao,
                taskDao,
                noteDao
            )
        }

        composable ("Profile"){ EmployeeProfilePage(navController, userDao, taskDao) }
       composable("Report") { ReportsScreen(navController,taskDao, truckDao, userDao, noteDao ) }

    }


}