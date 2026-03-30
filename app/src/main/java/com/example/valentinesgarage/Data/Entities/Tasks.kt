package com.example.valentinesgarage.Data.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Tasks")
data class Tasks (
    @PrimaryKey val Task: Int,
)