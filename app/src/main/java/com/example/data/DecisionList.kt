package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decision_lists")
data class DecisionList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
