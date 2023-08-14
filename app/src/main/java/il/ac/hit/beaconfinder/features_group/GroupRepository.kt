package il.ac.hit.beaconfinder.features_group

import il.ac.hit.beaconfinder.data.GroupDao
import il.ac.hit.beaconfinder.data.GroupEntity
import kotlinx.coroutines.flow.Flow

class GroupRepository(private val groupDao: GroupDao) {


    suspend fun isGroupEmpty() : Boolean{
        val count = groupDao.getCount()
     return count == 0
    }


     suspend fun getAllGroup() : Flow<List<GroupEntity>> {
       return  groupDao.getGroups()
     }

    suspend fun insert(group : GroupEntity): Long {
       return groupDao.insert(group)
    }

    suspend fun deleteAll() {
        groupDao.deleteAll()
    }

}
