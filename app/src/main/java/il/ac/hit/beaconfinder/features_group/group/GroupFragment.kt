package il.ac.hit.beaconfinder.features_group.group

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import il.ac.hit.beaconfinder.data.GroupEntity
import il.ac.hit.beaconfinder.feature_socialNetwork.FirebaseUserInfo
import il.ac.hit.beaconfinder.features_group.GroupPresenter
import il.ac.hit.beaconfinder.features_group.GroupView
import il.ac.hit.beaconfinder.features_group.GroupViewmodel
import il.ac.hit.beaconfinder.features_group.MemberInfo
import il.ac.hit.beaconfinder.features_group.groupInvite
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject


@AndroidEntryPoint
class GroupFragment : Fragment() , GroupView {

    companion object {
        private const val TAG = "GroupFragment"
        const val GroupID = "GroupID"
    }


    @Inject
    lateinit var groupViewmodel: GroupViewmodel

    var addGroupButton: Button? = null
    var createGroupButton: FloatingActionButton? = null
    var emptyGroupLayout: LinearLayout? = null
    var groupRecyclerView: RecyclerView? = null
    var progressBar: ProgressBar? = null

    lateinit var groupAdapter : GroupAdapter
    lateinit var presenter: GroupPresenter

    private var user: FirebaseUserInfo? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val view = inflater.inflate(il.ac.hit.beaconfinder.R.layout.fragment_group, container, false)


        presenter = GroupPresenter(this, groupViewmodel)


        addGroupButton = view.findViewById<Button>(il.ac.hit.beaconfinder.R.id.addGroupButton)

        createGroupButton = view.findViewById<FloatingActionButton>(il.ac.hit.beaconfinder.R.id.createGroupButton)
        emptyGroupLayout = view.findViewById<LinearLayout>(il.ac.hit.beaconfinder.R.id.emptyGroupLayout)
        groupRecyclerView = view.findViewById<RecyclerView>(il.ac.hit.beaconfinder.R.id.groupRecyclerView)
        progressBar = view.findViewById<ProgressBar>(il.ac.hit.beaconfinder.R.id.progressBar)


        addGroupButton!!.setOnClickListener { v ->

            if (user != null) {
                createGroupDialog(user!!)
            }

        }

        createGroupButton!!.setOnClickListener { v ->

            if (user != null) {
                createGroupDialog(user!!)
            }
        }

        showLoading()


        lifecycleScope.launch {
            try {
                getGroupFromfirebasse()
            } catch (e: Exception) {
                // Handle any exceptions that occur during the coroutine execution
            }
        }


        // Initialize the adapter with an empty list
        groupAdapter = GroupAdapter() {
            navigateToGroupChatFragment(it.groupFirebaseId)
        }

        // Create a coroutine scope using the viewLifecycleOwner
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe the Flow emitted by getGroup()
            groupViewmodel.getGroup().collect { groups ->
                // Handle the emitted groups here
                // Update UI, perform calculations, etc.
                if (groups.isNotEmpty()) {
                    Log.d(TAG, "onCreateView() called with: groups = ${groups.size}")
                    // Update the items in the adapter
                    updateList(groups)
                }
            }
        }


        lifecycleScope.launch(Dispatchers.IO) {
            user = FirebaseUtils().getCurrentUserdata()
            if (user != null ) {
            if (user!!.phoneNumber.isNotEmpty()) {
                    checkforGroupinvites(user!!.phoneNumber , user!!)
                }
            }

        }



        return view
    }


    suspend fun checkforGroupinvites(memberPhone: String, user: FirebaseUserInfo) {
        Log.d(TAG, "checkforGroupinvites() called with: memberPhone = $memberPhone, user = $user")
            val recordExists: Pair<String, groupInvite?> = FirebaseUtils().getRecordForPhone(memberPhone)
        Log.d(TAG, "checkforGroupinvites() called $recordExists")
        if (recordExists != null) {
            // A record exists with the given memberPhone
            // Access the record using `record` variable

            if (recordExists.second == null) {
                Log.d(
                    TAG,
                    "checkforGroupinvites() called with: memberPhone = $memberPhone, user = $user"
                )
                return
            }



            val group = FirebaseUtils().getGroupFromId( recordExists.second!!.groupId)



            if (group != null) {
                // todo update
                // delete it

                val creatorInfo = MemberInfo(
                    name = user.userName,
                    uuid = user.uid,
                    phone = user.phoneNumber
                )


               val updated =  if (group.members.isNotEmpty()) {
                   parseMemberInfoFromJson(group.members).toMutableList()
                   } else {
                    mutableListOf<MemberInfo>()
                }
                updated.add(creatorInfo)

                 val memeberInfo = Gson().toJson(updated)

                // update Group
                FirebaseUtils().updateGroup(recordExists.second!!.groupId,group.copy(members = memeberInfo))

                // Add to the userinfo
                FirebaseUtils().addGroupIdToUserInfo( recordExists.second!!.groupId)

                // delete the invite
                val group = FirebaseUtils().deleteRecord( recordExists.first)


                Log.i(TAG, "checkforGroupinvites: process Completed groupDeleted $group ")
            } else {
                Log.d(
                    TAG,
                    "checkforGroupinvites() called with: group = $group   ${recordExists.second!!.groupId}"
                )
            }

        } else {
            // No record exists with the given memberPhone
        }

    }

    fun parseMemberInfoFromJson(groupInfoJson: String): List<MemberInfo> {
        val gson = Gson()
        val arrayTutorialType = object : TypeToken<Array<MemberInfo>>() {}.type
        val groupEntityIdList: Array<MemberInfo> = gson.fromJson(groupInfoJson, arrayTutorialType)
        return groupEntityIdList.toList()
    }




  fun updateList(groups : List<GroupEntity>) {
      Log.d(TAG, "updateList() called with: groups = $groups")


      // Show the RecyclerView and hide other views
      progressBar!!.visibility = View.INVISIBLE
      groupRecyclerView!!.visibility = View.VISIBLE
      emptyGroupLayout!!.visibility = View.GONE

      // Update the items in the adapter
      groupAdapter.updateItems(groups)

      val ItemCount = groupAdapter.getItemCount()

      Log.i(TAG, "updateList: Count ${ItemCount}")


      // Set the adapter on the RecyclerView
      groupRecyclerView!!.adapter = groupAdapter
      // Set RecyclerView layout manager and adapter
      groupRecyclerView!!.layoutManager = LinearLayoutManager(context)
      // Notify the adapter that the data has changed
      // Then add the DividerItemDecoration
      val dividerItemDecoration = DividerItemDecoration(requireContext(), (groupRecyclerView!!.layoutManager as LinearLayoutManager).orientation)
      groupRecyclerView!!.addItemDecoration(dividerItemDecoration)



      groupAdapter.notifyDataSetChanged()


  }

    private fun navigateToGroupChatFragment(groupid : String) {
        Log.d(TAG, "navigateToGroupChatFragment() called with: groupid = $groupid")

        if (groupid.isNotEmpty() && view != null) {
            val bundle = Bundle()
            bundle.putString(GroupID, groupid)
            findNavController(view!!).navigate(il.ac.hit.beaconfinder.R.id.chatFragment_to_GroupChatFragment, bundle)

        }

    }

    override fun showLoading() {
        Log.d(TAG, "showLoading() called")

        // show loading
        progressBar!!.visibility = View.VISIBLE;
//        groupRecyclerView!!.visibility = View.GONE;
        emptyGroupLayout!!.visibility = View.GONE;

    }

    suspend fun getGroupFromfirebasse() {
        Log.d(TAG, "getGroupFromfirebasse() called")

       val userInfo  = groupViewmodel.MainRepository.firebase.getCurrentUserdata()

       val isNotEmpty = userInfo?.groupinfo?.isNotEmpty() ?: true

        if (isNotEmpty) {

            if (userInfo != null) {
//                groupViewmodel.updateGroup(userInfo)


                val list = groupViewmodel.parseGroupIdsFromJson(userInfo?.groupinfo)
                val result: List<GroupEntity> = groupViewmodel.getGroupEntitiesFromIds(list)
                updateList(result)



            }
            emptyGroupLayout!!.visibility = View.INVISIBLE
            // show loading
            progressBar!!.visibility = View.INVISIBLE;


        } else {
            progressBar!!.visibility = View.INVISIBLE;
            emptyGroupLayout!!.visibility = View.VISIBLE
        }



    }


    override fun hideLoading() {

    }

    override fun createGroupDialog(firebaseUserInfo :FirebaseUserInfo) {
        Log.d(TAG, "createGroupDialog() called")

        // Create the AlertDialog builder
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Create a new group")

        // Create the layout for the dialog
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL

        // Create the input fields and add them to the layout
        val groupNameInput = EditText(requireContext())
        groupNameInput.hint = "Group Name"
        layout.addView(groupNameInput)

        builder.setView(layout)

        // Create the buttons for the AlertDialog
        builder.setPositiveButton("Create") { dialog, _ ->
            val groupName = groupNameInput.text.toString()
            if (groupName.isNotEmpty()) {


                val creatorInfo = MemberInfo(
                    name = firebaseUserInfo.userName,
                    uuid = firebaseUserInfo.uid,
                    phone = firebaseUserInfo.phoneNumber
                )

                val group = GroupEntity(
                    name = groupName,
                    ownerInfo = Gson().toJson(creatorInfo),
                    groupCreatedDate = Date().time
                )

                lifecycleScope.launch {
                    val result =
                        withContext(Dispatchers.Default) {
                            FirebaseUtils().createGroup(group)
                        }
                    if (result != null) {
                        groupViewmodel.insertGroup(result)
                        FirebaseUtils().addGroupIdToUserInfo(result.groupFirebaseId)
                    }
                    // add this group info yto the user

                    dialog.dismiss()
                }
            } else {
                Toast.makeText(requireContext(), "Group name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        // Create and show the AlertDialog
        val alertDialog = builder.create()
        alertDialog.show()
    }

    override fun updateGroups(groups: List<GroupEntity>) {




    }


}