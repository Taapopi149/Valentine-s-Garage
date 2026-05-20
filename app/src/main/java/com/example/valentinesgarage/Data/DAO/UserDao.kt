package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.Entities.User
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the users table.
 * Handles database operations related to employees and managers.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getUserById(employeeId: String) : User?

    @Query("SELECT employeeId FROM users ORDER BY employeeId DESC LIMIT 1")
    suspend fun getLastEmployeeId(): String?

    /**
     * Finds the last Employee ID that follows the 'EMP' prefix pattern.
     * Used for auto-generating the next sequential ID (e.g., EMP001 -> EMP002).
     */
    @Query("""
    SELECT employeeId FROM users
    WHERE employeeId LIKE 'EMP%'
    ORDER BY CAST(SUBSTR(employeeId, 4) AS INTEGER) DESC
    LIMIT 1
""")
    suspend fun getLastEmployeeIdOnly(): String?

    @Query("SELECT * FROM users WHERE employeeId = :employeeId AND password = :password LIMIT 1")
    suspend fun login(employeeId: String, password: String) : User?

    /**
     * Observes all users in the database.
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

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

    @Query("SELECT * FROM users WHERE employeeId = :id")
    fun getUserId(id: String): Flow<User?>

    @Query("""
    UPDATE users SET 
    first_name  = :firstName,
    last_name   = :lastName,
    email       = :email,
    phone       = :phone,
    Department  = :department,
    shift       = :shift
    WHERE employeeId = :employeeId
""")
    suspend fun updateUser(
        employeeId:  String,
        firstName:   String,
        lastName:    String,
        email:       String,
        phone:       String,
        department:  String,
        shift:       String
    )

    @Query("SELECT first_name || ' ' || last_name FROM users WHERE employeeId = :id")
    suspend fun getFullNameById(id: String): String?
}