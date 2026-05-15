package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.Entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getUserById(employeeId: String) : User?

    @Query("SELECT employeeId FROM users ORDER BY employeeId DESC LIMIT 1")
    suspend fun getLastEmployeeId(): String?

    @Query("""
    SELECT employeeId FROM users
    WHERE employeeId LIKE 'EMP%'
    ORDER BY CAST(SUBSTR(employeeId, 4) AS INTEGER) DESC
    LIMIT 1
""")
    suspend fun getLastEmployeeIdOnly(): String?

    @Query("SELECT * FROM users WHERE employeeId = :employeeId AND password = :password LIMIT 1")
    suspend fun login(employeeId: String, password: String) : User?

    @Query("SELECT * FROM users WHERE role = :role")
    suspend fun getUsersByRole(role: String): List<User>

    @Query("""
    SELECT * FROM users WHERE 
    first_name LIKE '%' || :query || '%' OR
    last_name  LIKE '%' || :query || '%' OR
    employeeId LIKE '%' || :query || '%' OR
    role       LIKE '%' || :query || '%' OR
    Department LIKE '%' || :query || '%'
""")
    fun searchUsers(query: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = 'mechanic' OR role = 'Mechanic'")
    fun getAllMechanics(): Flow<List<User>>


}