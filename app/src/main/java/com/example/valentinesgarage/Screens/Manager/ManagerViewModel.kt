package com.example.valentinesgarage.Screens.Manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Manager.Mechanic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Manager dashboard.
 * Manages staff data by observing the database and exposing it as a StateFlow for the UI.
 */
class ManagerViewModel(private val userDao: UserDao) : ViewModel() {

    /**
     * A flow of all registered employees, converted from the User entity to the UI-specific Mechanic model.
     * Updates automatically whenever changes occur in the users table.
     */
    val employees: StateFlow<List<Mechanic>> = userDao.getAllUsers()
        .map { users ->
            users.map { user ->
                Mechanic(
                    employeeId = user.employeeId,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    role = user.role,
                    shift = user.shift ?: "N/A",
                    isActive = true // Defaulting to true for now
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}