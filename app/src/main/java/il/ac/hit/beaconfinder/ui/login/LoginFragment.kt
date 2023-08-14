package il.ac.hit.beaconfinder.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import il.ac.hit.beaconfinder.MainActivity
import il.ac.hit.beaconfinder.R
import il.ac.hit.beaconfinder.databinding.FragmentLoginBinding
import il.ac.hit.beaconfinder.firebase.FirebaseUtils


class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        implementOnClickListeners()
        implementOnClickListenersSkip()
        implementOnClickListenersLogin()

        // Check if the Firebase user is already logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // If the user is logged in, navigate to the main screen
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

        }

    }

    private fun implementOnClickListeners() {
        binding.tvRegister.setOnClickListener {
            NavHostFragment.findNavController(this).navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun implementOnClickListenersSkip() {
        binding.skip.setOnClickListener {

            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)

        }
    }

    private fun implementOnClickListenersLogin() {
        binding.buttonLogin.setOnClickListener {

            val email = binding.editTextLoginEmail.editText?.text.toString()
            val password = binding.editTextLoginPass.editText?.text.toString()


            if (validateInput(email, password)) {
                FirebaseUtils().signin(email, password) {
                    if (it.isSuccessful) {
                        Toast.makeText(context, "Sign in successful", Toast.LENGTH_LONG).show()
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            context,
                            "Sign in unsuccessful: ${it.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    private fun validateInput(email: String, password: String): Boolean {
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextLoginEmail.error = "Please enter a valid email"
            return false
        } else {
            binding.editTextLoginEmail.error = null
        }

        if (TextUtils.isEmpty(password) || password.length < 6) {
            binding.editTextLoginPass.error = "Password must be at least 6 characters long"
            return false
        } else {
            binding.editTextLoginPass.error = null
        }

        return true
    }


}