package com.example.valentinesgarage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.AppNavigation.AppNavigation
import com.example.valentinesgarage.Data.Database.AppDatabase
import com.example.valentinesgarage.Screens.CheckIn.CheckInViewModel
import com.example.valentinesgarage.Screens.Mechanic.MechanicViewModel
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesViewModel
import kotlin.jvm.java


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val userDao = database.userDao()
        val truckDao = database.TruckDao()
        val noteDao = database.NotesDao()
        val taskDao = database.TasksDao()





        enableEdgeToEdge()
        setContent {
            AppNavigation(userDao = userDao, truckDao = truckDao, noteDao = noteDao, taskDao =taskDao)
        }
    }
}
