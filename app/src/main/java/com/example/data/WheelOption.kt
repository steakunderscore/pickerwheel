package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wheel_options",
    foreignKeys = [
        ForeignKey(
            entity = DecisionList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class WheelOption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val label: String,
    val colorHex: String,
    val weight: Float = 1.0f
)
