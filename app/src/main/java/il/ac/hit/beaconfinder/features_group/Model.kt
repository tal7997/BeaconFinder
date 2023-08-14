package il.ac.hit.beaconfinder.features_group

data class MemberInfo(
 var name : String,
 var phone: String,
 var uuid: String,

)



data class groupInvite(
    var createTime : Long ,
    var memberPhone : String,
    var groupId : String
)