package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.Entities.Truck
import com.example.valentinesgarage.Screens.Vehicles.TruckStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface  TruckDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTruck(truck: Truck): Long

    @Query("SELECT * FROM Truck")
    fun getAllTruck(): Flow<List<Truck>>


    @Query("SELECT * FROM Truck ORDER BY truckId DESC")
    fun getAllTrucks(): Flow<List<Truck>>

    @Query("SELECT noteText FROM Notes WHERE truckIdOwner = :truckId LIMIT 1")
    suspend fun getNoteForTruck(truckId: Int): String?

    @Query("SELECT COUNT(*) FROM Tasks WHERE truckIdOwner = :truckId")
    suspend fun getTaskCountForTruck(truckId: Int): Int

    @Query("""
    SELECT description FROM Tasks 
    WHERE truckIdOwner = :truckId AND status != 'Done'
    ORDER BY Taskid DESC 
    LIMIT 1
""")
    suspend fun getCurrentTaskForTruck(truckId: Int): String?

    @Query("UPDATE Truck SET truckStatus = :status WHERE truckId = :truckId")
    suspend fun updateTruckStatus(truckId: Int, status: TruckStatus)

    @Query("""
    SELECT * FROM Truck WHERE
    licencePlate LIKE '%' || :query || '%' OR
    DriverName   LIKE '%' || :query || '%'
""")
    fun searchTrucks(query: String): Flow<List<Truck>>


    @Query("SELECT COUNT(*) FROM Truck")
    fun getTruckCount(): Flow<Int>

    @Query("SELECT * FROM Truck WHERE truckId = :truckId")
    fun getTruckById(truckId: Int): Flow<Truck?>





}