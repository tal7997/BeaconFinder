package il.ac.hit.beaconfinder.features_group

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import il.ac.hit.beaconfinder.data.GroupEntity
import il.ac.hit.beaconfinder.data.MainRepository
import il.ac.hit.beaconfinder.feature_socialNetwork.FirebaseUserInfo
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewmodel@Inject constructor(
    @ApplicationContext val context: Context,
    val MainRepository: MainRepository
)  : ViewModel() {

    companion object {
        private const val TAG = "Groupviewmodel"
    }


    // group


    var groupRepository =  GroupRepository(MainRepository.tagsDb.GroupDao())


    suspend fun checkGroupIsEmpty() : Boolean {
        return  groupRepository.isGroupEmpty()
    }

    fun insertGroup(group : GroupEntity) {

        viewModelScope.launch {

            // Step 1
          var groupid=   groupRepository.insert(group)

            // Step 2
//            MainRepository.firebase.createGroup(group.copy(id=groupid.toInt()))
//

        }



    }


    fun parseGroupIdsFromJson(groupInfoJson: String): List<String> {
        val gson = Gson()
        val arrayTutorialType = object : TypeToken<Array<String>>() {}.type
        val groupEntityIdList: Array<String> = gson.fromJson(groupInfoJson, arrayTutorialType)
        return groupEntityIdList.toList()
    }





    suspend fun updateGroup(userInfo: FirebaseUserInfo): Boolean {
        Log.d(TAG, "updateGroup() called with: userInfo = $userInfo")
        return coroutineScope {
            // Step 1: Delete
            groupRepository.deleteAll()
            // Step 2: Insert

            if (userInfo.groupinfo.isNotEmpty()) {
                val gson = Gson()
                val arrayTutorialType = object : TypeToken<Array<String>>() {}.type

                val groupEntityIdList: Array<String> = gson.fromJson(userInfo.groupinfo, arrayTutorialType)

                if (groupEntityIdList.isNotEmpty()) {
                    groupEntityIdList.map { groupId ->
                        async {
                            val data: GroupEntity? = FirebaseUtils().getGroupFromId(groupId) // suspend function

                            if (data != null) {
                                groupRepository.insert(data) // suspend function
                            }
                        }
                    }.forEach { deferred ->
                        deferred.await()
                    }

                    return@coroutineScope true
                }
            }
            return@coroutineScope false
        }
    }

    suspend fun getGroupEntitiesFromIds(groupIds: List<String>): List<GroupEntity> = coroutineScope {
        val deferredResults = groupIds.map { groupId ->
            async {
                FirebaseUtils().getGroupFromId(groupId)
            }
        }

        deferredResults.awaitAll()
            .filterNotNull()
    }



     suspend fun getGroup(): Flow<List<GroupEntity>> {
        return  groupRepository.getAllGroup()
     }

}