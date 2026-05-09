package com.example.valentinesgarage.Screens.Converters

import androidx.room.TypeConverter
import com.example.valentinesgarage.Screens.CheckIn.VehicleCondition

class EnumCon {

    @TypeConverter
    fun fromCondition(condition: VehicleCondition): String {
        return condition.name
    }

    @TypeConverter
    fun toCondition(value: String): VehicleCondition {
        return VehicleCondition.valueOf(value)
    }
}