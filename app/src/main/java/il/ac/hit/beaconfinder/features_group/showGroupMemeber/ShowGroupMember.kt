package il.ac.hit.beaconfinder.features_group.showGroupMemeber

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.features_group.group.GroupFragment
import il.ac.hit.beaconfinder.features_group.group_chat.GroupChatFragment
import il.ac.hit.beaconfinder.features_group.group_chat.GroupChatFragment.Companion.GroupId
import il.ac.hit.beaconfinder.features_group.group_chat.GroupChatViewModel

class ShowGroupMember : Fragment() {


    private lateinit var viewModel: GroupChatViewModel
    companion object {
        private const val TAG = "ShowGroupMember"
    }
    private lateinit var adapter: GroupMemberAdapter

    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_group_members, container, false)
        viewModel = ViewModelProvider(this)[GroupChatViewModel::class.java]

        adapter = GroupMemberAdapter(emptyList())

        GroupId = arguments?.getString(GroupChatFragment.GroupId) ?: throw Exception("Exception")

        // Use the groupId as needed

        viewModel.listenForMemeberstatus(GroupId)

        recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Then add the DividerItemDecoration
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, (recyclerView.layoutManager as LinearLayoutManager).orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)

        recyclerView.adapter = adapter

        observeMembers()


        return view
    }

    private fun observeMembers() {
        viewModel.groupMember.observe(viewLifecycleOwner) { chatMessages ->
            adapter.updateMembers(chatMessages)
            recyclerView.scrollToPosition(chatMessages.size - 1)
        }
    }


}