package com.example.missingpeople.repositor

import java.io.Serializable
import java.util.Date
import java.util.UUID

class MissingPerson (
    val name: String,
    val description: String,
    val birthDate: Date?,
    val disappearanceDate: Date?,
    val gender: String,
    val photos: String,
    val url: String
):Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}