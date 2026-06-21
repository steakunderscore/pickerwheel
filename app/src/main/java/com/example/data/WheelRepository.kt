package com.example.data

import kotlinx.coroutines.flow.Flow

class WheelRepository(private val wheelDao: WheelDao) {
    val allListsWithFlow: Flow<List<ListWithOption>> = wheelDao.getAllListsWithFlow()

    suspend fun insertList(title: String, options: List<Pair<String, String>>): Long {
        val listId = wheelDao.insertList(DecisionList(title = title))
        val optionsList = options.map {
            WheelOption(
                listId = listId,
                label = it.first,
                colorHex = it.second
            )
        }
        wheelDao.insertOptions(optionsList)
        return listId
    }

    suspend fun deleteListById(listId: Long) {
        wheelDao.deleteListById(listId)
    }

    suspend fun updateListWithOptions(listId: Long, title: String, options: List<Pair<String, String>>) {
        wheelDao.updateList(DecisionList(id = listId, title = title))
        wheelDao.deleteOptionsByListId(listId)
        val optionsList = options.map {
            WheelOption(
                listId = listId,
                label = it.first,
                colorHex = it.second
            )
        }
        wheelDao.insertOptions(optionsList)
    }
}
