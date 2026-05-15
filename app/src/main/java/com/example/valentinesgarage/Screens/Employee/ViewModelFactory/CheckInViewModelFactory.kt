package com.example.valentinesgarage.Screens.Employee.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.NotesDao
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Screens.CheckIn.CheckInViewModel

class CheckInViewModelFactory(
    private val truckDao: TruckDao,
    private val notesDao: NotesDao,
    private val tasksDao: TasksDao,
    private val userDao: UserDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckInViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
          return CheckInViewModel(
    truckDao,
    tasksDao, 
    notesDao,  
    userDao
) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
