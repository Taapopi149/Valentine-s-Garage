package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.Entities.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getUserById(employeeId: String) : User?

    @Query("SELECT employeeId FROM users ORDER BY employeeId DESC LIMIT 1")
    suspend fun getLastEmployeeId(): String?

    @Query("SELECT * FROM users WHERE employeeId = :employeeId AND password = :password LIMIT 1")
    suspend fun login(employeeId: String, password: String) : User?


}