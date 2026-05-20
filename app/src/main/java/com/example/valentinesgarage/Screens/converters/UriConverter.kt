package com.example.valentinesgarage.Screens.converters

import android.net.Uri

fun List<Uri>.toStorageString(): String =
    joinToString(",") { it.toString() }

fun String.toUriList(): List<Uri> =
    if (isEmpty()) emptyList() else split(",").map { Uri.parse(it) }