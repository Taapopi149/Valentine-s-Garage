package com.example.valentinesgarage

import com.example.valentinesgarage.Data.DAO.UserDao
import com.example.valentinesgarage.Data.Entities.User


object SessionManager {
    var currentUser : User? = null
        private set


    fun login(user: User){
        currentUser = user
    }

    fun logout() {
        currentUser = null
    }

}