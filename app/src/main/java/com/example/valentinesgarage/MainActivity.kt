package com.example.valentinesgarage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.valentinesgarage.AppNavigation.AppNavigation
import com.example.valentinesgarage.Data.Database.AppDatabase



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
