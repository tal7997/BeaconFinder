package il.co.syntax.firebasemvvm.repository

import il.co.syntax.firebasemvvm.model.User
import il.co.syntax.myapplication.util.Resource

interface AuthRepository {

    suspend fun currentUser() : Resource<User>
    suspend fun login(email:String, password:String) : Resource<User>
    suspend fun createUser(userName:String,
                           userEmail:String,
                           userPhone:String,
                           userLoginPass:String) : Resource<User>
    fun logout()
}