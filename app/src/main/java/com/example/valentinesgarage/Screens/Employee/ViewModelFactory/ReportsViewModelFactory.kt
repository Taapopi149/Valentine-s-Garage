package com.example.valentinesgarage.Screens.Employee.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.NotesDao
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Screens.Manager.ReportsViewModel


class ReportsViewModelFactory(
    private val tasksDao: TasksDao,
    private val truckDao: TruckDao,
    private val userDao: UserDao,
    private val notesDao: NotesDao

) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(
                tasksDao,
                truckDao,
                userDao,
                notesDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}