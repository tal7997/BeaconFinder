package il.ac.hit.beaconfinder.ui.register

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.databinding.FragmentRegisterBinding
import il.ac.hit.beaconfinder.firebase.FirebaseUtils

//import il.co.syntax.firebasemvvm.R
//import il.co.syntax.firebasemvvm.databinding.FragmentLoginBinding
//import il.co.syntax.firebasemvvm.databinding.FragmentRegisterBinding
//import il.co.syntax.firebasemvvm.repository.FirebaseImpl.AuthRepositoryFirebase
//import il.co.syntax.fullarchitectureretrofithiltkotlin.utils.autoCleared
//import il.co.syntax.myapplication.util.Resource

class RegisterFragment : Fragment(){

    private lateinit var binding: FragmentRegisterBinding


    companion object {
        private const val TAG = "RegisterFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater,container,false)

        binding.userRegisterButton.setOnClickListener {


            Firebase.messaging.token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result


//            viewModel.createUser(binding.edxtUserName.editText?.text.toString(),
//             binding.edxtEmailAddress.editText?.text.toString(),
//             binding.edxtPhoneNum.editText?.t ext.toString(),
//             binding.edxtPassword.editText?.text.toString())



                val firebaseUtil = FirebaseUtils()
                val email = binding.edxtEmailAddress.editText?.text.toString()
                val password = binding.edxtPassword.editText?.text.toString()
                val userName = binding.edxtUserName.editText?.text.toString()
                val phoneNumber = binding.edxtPhoneNum.editText?.text.toString()
                firebaseUtil.signup(email= email, password= password,userName= userName, token = token,phoneNumber= phoneNumber, {
                    Toast.makeText(context, "Sign up successful", Toast.LENGTH_LONG)
                        .show()
                    NavHostFragment.findNavController(this).navigate(R.id.action_registerFragment_to_loginFragment)
                }, {
                    Toast.makeText(
                        context, "Sign up unsuccessful: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                })
            }

        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}