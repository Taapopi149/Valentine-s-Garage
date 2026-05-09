package com.example.valentinesgarage.Data.Entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Notes" ,
    foreignKeys =  [
        ForeignKey(
            entity = Truck::class,
            parentColumns = ["licencePlate"],
            childColumns = ["truckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("truckId")]
)


data class Notes (
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0,
    val truckId: String,
    val noteText: String
)