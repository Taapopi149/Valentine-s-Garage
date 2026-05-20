package com.example.valentinesgarage.Screens.Employee.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.valentinesgarage.Data.DAO.TruckDao
import com.example.valentinesgarage.Screens.Vehicles.ActiveVehiclesViewModel


class ActiveVehiclesViewModelFactory(
    private val truckDao: TruckDao,

    ) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveVehiclesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActiveVehiclesViewModel(
                truckDao,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}