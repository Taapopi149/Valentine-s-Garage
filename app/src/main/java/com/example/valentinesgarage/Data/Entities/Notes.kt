package com.example.valentinesgarage.Data.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Notes")
data class Notes (
    @PrimaryKey val NoteId: Int,
)