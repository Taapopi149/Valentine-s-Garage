package com.example.valentinesgarage.Data.Entities

import androidx.room.*
import android.net.Uri

@Entity(tableName = "users")
data class  User (
    @PrimaryKey val employeeId: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name= "shift") val shift: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "join_date") val joinDate: String?,
    @ColumnInfo(name = "tasks_completed") val taskCompleted: Int?,
    @ColumnInfo(name = "tasks_pending") val tasksPending: Int?,
    @ColumnInfo(name="task_progress") val taskProgress: Int?,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "Department") val department: String?

)