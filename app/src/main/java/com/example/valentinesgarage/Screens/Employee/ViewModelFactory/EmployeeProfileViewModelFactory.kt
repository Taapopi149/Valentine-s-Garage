package com.example.valentinesgarage.Screens.Employee.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Screens.Employee.EmployeeProfileViewModel

class EmployeeProfileViewModelFactory(
    private val userDao: UserDao,
    private val tasksDao: TasksDao,
    private val employeeId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EmployeeProfileViewModel(userDao, tasksDao, employeeId) as T
    }
}