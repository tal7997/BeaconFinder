package il.ac.hit.beaconfinder.features_group

import il.ac.hit.beaconfinder.data.GroupEntity
import il.ac.hit.beaconfinder.feature_socialNetwork.FirebaseUserInfo

interface GroupView {

    fun showLoading()
    fun hideLoading()
    fun createGroupDialog(firebaseUserInfo : FirebaseUserInfo)
    fun updateGroups(groups: List<GroupEntity>)

}