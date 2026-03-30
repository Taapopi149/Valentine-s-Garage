package com.example.valentinesgarage.Data.Database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.valentinesgarage.Data.Entities.Notes
import com.example.valentinesgarage.Data.Entities.Tasks
import com.example.valentinesgarage.Data.Entities.Truck
import com.example.valentinesgarage.Data.Entities.User

@Database(entities = [User::class, Notes::class, Tasks::class, Truck::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

}