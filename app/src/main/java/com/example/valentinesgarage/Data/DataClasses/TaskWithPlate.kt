package com.example.valentinesgarage.Data.DataClasses

import androidx.room.ColumnInfo

data class TaskWithPlate(

    @ColumnInfo(name = "Taskid")
    val taskId: Int,
    val description: String,
    val status: String,
    val priority: String,
    val employeeIdFk: String,
    val truckIdOwner: Int,
    val licencePlate: String
)