package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class ListWithOption(
    @Embedded val decisionList: DecisionList,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val options: List<WheelOption>
)

@Dao
interface WheelDao {
    @Transaction
    @Query("SELECT * FROM decision_lists ORDER BY createdAt DESC")
    fun getAllListsWithFlow(): Flow<List<ListWithOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(decisionList: DecisionList): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: WheelOption): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<WheelOption>)

    @Query("DELETE FROM decision_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    @Query("DELETE FROM wheel_options WHERE listId = :listId")
    suspend fun deleteOptionsByListId(listId: Long)

    @Update
    suspend fun updateList(decisionList: DecisionList)
}
