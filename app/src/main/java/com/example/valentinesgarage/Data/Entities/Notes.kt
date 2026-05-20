package com.example.valentinesgarage.Data.Entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Notes" ,
    foreignKeys =  [
        ForeignKey(
            entity = Truck::class,
            parentColumns = ["truckId"],
            childColumns = ["truckIdOwner"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("truckIdOwner")]
)


data class Notes (
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0,
    val truckIdOwner: Int,
    val noteText: String
)