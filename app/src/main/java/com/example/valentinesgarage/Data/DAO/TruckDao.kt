package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.Entities.Truck
import kotlinx.coroutines.flow.Flow

@Dao
interface  TruckDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTruck(truck: Truck): Long

    @Query("SELECT *  FROM Truck")
    suspend fun getAllTruck() : Flow<List<Truck>>

}