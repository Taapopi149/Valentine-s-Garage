package com.example.valentinesgarage.Screens.Employee.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Screens.Employee.EmployeeHomeViewModel


class EmployeeHomeViewModelFactory(
    private val truckDao: TruckDao,
    private val tasksDao: TasksDao,
    private val employeeId: String

) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmployeeHomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmployeeHomeViewModel(
                tasksDao,
                truckDao,
                employeeId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}