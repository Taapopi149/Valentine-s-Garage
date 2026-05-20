package com.example.valentinesgarage


import com.example.valentinesgarage.Data.Entities.User


object SessionManager {
    var currentUser : User? = null
        private set

    var currentUserId: String? = null

    fun login(user: User){
        currentUser = user
        currentUserId = user.employeeId

    }

    fun logout() {
        currentUser = null
        currentUserId = null
    }

}