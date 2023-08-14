package il.ac.hit.beaconfinder.features_group

import android.view.ViewGroup
import il.ac.hit.beaconfinder.data.MainRepository
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupPresenter(private val view: GroupView, private val GroupViewmodel: GroupViewmodel) {

    companion object {
        private const val TAG = "GroupPresenter"
    }

    private var job: Job? = null

    fun loadData() {
        view.showLoading()

        job = CoroutineScope(Dispatchers.IO).launch {
            delay(3000)
//            repository.refreshGroups()
            withContext(Dispatchers.Main) {
                view.hideLoading()
            }
        }
    }

    fun stop() {
        job?.cancel()
    }


    suspend fun checkGroupIsEmpty() : Boolean {
        return  GroupViewmodel.checkGroupIsEmpty()
    }




}