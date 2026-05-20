package com.example.valentinesgarage.Screens.Employee.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Screens.Mechanic.MechanicViewModel


class MechanicViewModelFactory(
    private val truckDao: TruckDao,
    private val tasksDao: TasksDao,
    private val userDao: UserDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MechanicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MechanicViewModel(
                truckDao,
                tasksDao,
                userDao

            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}