//package il.ac.hit.beaconfinder.viewmodels
//
//import androidx.lifecycle.*
//import il.ac.hit.beaconfinder.data.MainRepository
//import il.ac.hit.beaconfinder.firebase.FirebaseUtils
//import il.co.syntax.myapplication.util.Resource
//import kotlinx.coroutines.launch
//
//import il.co.syntax.firebasemvvm.model.User
//import il.co.syntax.firebasemvvm.repository.AuthRepository
//import il.co.syntax.firebasemvvm.ui.register.RegisterViewModel
//import javax.inject.Inject
//
//class LoginViewModel @Inject constructor(
//    private val firebaseUtils: FirebaseUtils
//) : ViewModel() {
//
//    private val _userSignInStatus = MutableLiveData<Resource<User>>()
//    val userSignInStatus: LiveData<Resource<User>> = _userSignInStatus
//
//    private val _currentUser = MutableLiveData<Resource<User>>()
//    val currentUser:LiveData<Resource<User>> = _currentUser
//
//    init {
//        viewModelScope.launch {
//            _currentUser.postValue(Resource.Loading())
//            _currentUser.postValue(authRep.currentUser())
//        }
//    }
//
//    fun signInUser(userEmail:String, userPass:String) {
//        if(userEmail.isEmpty() || userPass.isEmpty())
//            _userSignInStatus.postValue(Resource.Error("Empty email or password"))
//        else{
//            _userSignInStatus.postValue(Resource.Loading())
//            firebaseUtils.login(activity, userEmail, userPass)
//                .addOnCompleteListener(activity) { task ->
//                if (task.isSuccessful) {
//                    // Sign in success, update UI with the signed-in user's information
//                    viewModelScope.launch {
//                        val loginResult = authRep.login(userEmail,userPass)
//                        _userSignInStatus.postValue(loginResult)
//                    }
//                    Log.d("FirebaseUtils", "signInWithEmailAndPassword:success")
//                    user = auth.currentUser!!
//                } else {
//                    Log.e("FirebaseUtils", "signInWithEmailAndPassword:failure", task.exception)
//                }
//            }
//        }
//    }
//
//
//    class LoginViewModelFactory(private val repo: AuthRepository) : ViewModelProvider.NewInstanceFactory() {
//
//        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//            return LoginViewModel(repo) as T
//        }
//    }
//}