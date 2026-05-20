package com.example.valentinesgarage.Screens.Manager.ViewFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Screens.Manager.ManagerViewModel

class ManagerViewModelFactory(private val userDao: UserDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManagerViewModel(userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}