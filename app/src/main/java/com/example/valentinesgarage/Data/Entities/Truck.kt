package com.example.valentinesgarage.Data.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.valentinesgarage.Screens.CheckIn.VehicleCondition
import com.example.valentinesgarage.Screens.Vehicles.TruckStatus

@Entity(tableName = "Truck")
data class Truck (
    @PrimaryKey val licencePlate: String,
    val DriverName: String,
    val Odmeter: Int,
    val Condition: VehicleCondition,
    val truckStatus: TruckStatus,

    //Store Image as String
    val photoUris: String,
)