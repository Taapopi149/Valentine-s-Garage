package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.DataClasses.TaskEmployeeEntry
import com.example.valentinesgarage.Data.DataClasses.TaskLabelCount
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

    @Query("SELECT COUNT(*) FROM Tasks WHERE status != 'Done'")
    fun getActiveTaskCount(): Flow<Int>

    @Query("""
    SELECT COUNT(*) FROM Tasks 
    WHERE employeeIdFk = :employeeId AND status = :status
""")
    suspend fun getTaskCountByStatus(employeeId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM Tasks WHERE status = 'Done'")
    fun getCompletedTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Tasks WHERE status = 'Pending'")
    fun getPendingTaskCount(): Flow<Int>

    @Query("""
    SELECT Tasks.employeeIdFk, Tasks.description, Tasks.status, Tasks.truckIdOwner
    FROM Tasks
""")
    fun getAllTasksWithEmployee(): Flow<List<TaskEmployeeEntry>>

    @Query("""
    SELECT Tasks.description as taskLabel, COUNT(DISTINCT Tasks.truckIdOwner) as truckCount
    FROM Tasks
    WHERE Tasks.employeeIdFk = :employeeId
    GROUP BY Tasks.description
""")
    suspend fun getTaskSummaryForEmployee(employeeId: String): List<TaskLabelCount>


    @Query("""
    SELECT Notes.noteText FROM Notes
    INNER JOIN Truck ON Notes.truckIdOwner = Truck.truckId
    INNER JOIN Tasks ON Tasks.truckIdOwner = Truck.truckId
    WHERE Tasks.employeeIdFk = :employeeId
    LIMIT 1
""")
    suspend fun getLatestNoteForEmployee(employeeId: String): String?

    @Query("SELECT COUNT(*) FROM Tasks WHERE employeeIdFk = :employeeId")
    suspend fun getTotalTasksForEmployee(employeeId: String): Int

}