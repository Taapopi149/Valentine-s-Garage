package com.example.valentinesgarage.Data.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Truck")
data class Truck (
    @PrimaryKey val TruckId: Int,
)