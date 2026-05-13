package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.DataClasses.TaskWithPlate
import com.example.valentinesgarage.Data.Entities.Tasks
import kotlinx.coroutines.flow.Flow


@Dao
interface TasksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Tasks)

    @Query("""
    SELECT Tasks.Taskid, Tasks.description, Tasks.status, Tasks.priority,
           Tasks.employeeIdFk, Tasks.truckIdOwner, Truck.licencePlate
    FROM Tasks
    INNER JOIN Truck ON Tasks.truckIdOwner = Truck.truckId
    WHERE Tasks.employeeIdFk = :employeeId
    ORDER BY Tasks.Taskid DESC
    LIMIT 4
""")
    fun getRecentTasksForEmployee(employeeId: String): Flow<List<TaskWithPlate>>


    @Query("SELECT * FROM Tasks WHERE truckIdOwner = :truckId")
    fun getTasksForTruck(truckId: Int): Flow<List<Tasks>>

    @Query("UPDATE Tasks SET status = :status WHERE Taskid = :taskId")
    suspend fun updateTaskStatus(taskId: Int, status: String)

    @Query("""
    UPDATE Tasks 
    SET status = :status, completedBy = :completedBy, completedAt = :completedAt, note = :note 
    WHERE Taskid = :taskId
""")
    suspend fun markTaskComplete(taskId: Int, status: String, completedBy: String, completedAt: String, note: String)

    @Query("SELECT * FROM Tasks WHERE truckIdOwner = :truckId")
     suspend fun getTasksForTruckOnce(truckId: Int): List<Tasks>


    @Query("SELECT * FROM Tasks")
    fun getAllTasks(): Flow<List<Tasks>>

    @Query("SELECT * FROM Tasks WHERE employeeIdFk =:employeeId")
    fun getTasksForEmployee(employeeId: String): Flow<List<Tasks>>



}