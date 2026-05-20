package com.example.valentinesgarage.Data.Entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Tasks",

    foreignKeys = [
        ForeignKey(
            entity  = Truck::class,
            parentColumns = ["truckId"],
            childColumns = ["truckIdOwner"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity  = User::class,
            parentColumns = ["employeeId"],
            childColumns = ["employeeIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("truckIdOwner"), Index("employeeIdFk")]
    )
data class Tasks (
    @PrimaryKey(autoGenerate = true) val Taskid: Int = 0,
    val description: String,
    val employeeIdFk: String,
    val truckIdOwner: Int,
    val priority: String,
    val status: String,
    val completedBy: String = "",
    val completedAt: String = "",
    val note: String = ""
)