package com.example.valentinesgarage.Screens.Employee.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.NotesDao
import com.example.valentinesgarage.Data.DAO.TasksDao
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Screens.Vehicles.TruckDetailViewModel


class TruckDetailViewModelFactory(
    private val truckDao: TruckDao,
    private val tasksDao: TasksDao,
    private val notesDao: NotesDao,
    private val truckId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TruckDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TruckDetailViewModel(
                truckDao, tasksDao, notesDao, truckId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}