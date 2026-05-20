package com.example.valentinesgarage.Data.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.valentinesgarage.Data.Entities.Notes

@Dao
interface NotesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Notes)

    @Query("SELECT noteText FROM Notes WHERE truckIdOwner = :truckId LIMIT 1")
    suspend fun getNoteForTruck(truckId: Int): String?
}