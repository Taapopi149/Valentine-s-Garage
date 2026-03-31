package com.example.valentinesgarage.Screens.Manager.ViewFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Screens.Manager.AddEmployeeViewModel


class AddEmployeeViewModelFactory(private val userDao: UserDao): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEmployeeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEmployeeViewModel(userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}