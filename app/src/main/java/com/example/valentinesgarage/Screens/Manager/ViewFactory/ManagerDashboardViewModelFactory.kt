package com.example.valentinesgarage.Screens.Manager.ViewFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Manager.ManagerDashboardViewModel
import kotlin.jvm.java


class ManagerDashboardViewModelFactory(private val userDao: UserDao, private val truckDao: TruckDao, val tasksDao: TasksDao): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManagerDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManagerDashboardViewModel(userDao, truckDao, tasksDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}