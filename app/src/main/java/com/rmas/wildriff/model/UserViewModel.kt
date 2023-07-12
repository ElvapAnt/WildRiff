package com.rmas.wildriff.model

import android.app.Application
import android.net.Uri
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.rmas.wildriff.data.Riff
import com.rmas.wildriff.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.log

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://wildriff-84a61-default-rtdb.europe-west1.firebasedatabase.app/")
    private val usersRef = database.getReference("users")
    private val storage : FirebaseStorage = FirebaseStorage.getInstance("gs://wildriff-84a61.appspot.com")

    private val _profileImageFile: MutableLiveData<File?> = MutableLiveData()
    val profileImageFile: LiveData<File?> get() = _profileImageFile
    private val _loggedInUser: MutableLiveData<User?> = MutableLiveData()
    val loggedInUser: LiveData<User?> get() = _loggedInUser

    private val _userDataList = MutableLiveData<List<User>>()
    val userDataList: LiveData<List<User>> get() = _userDataList

    fun fetchUserData(userId: String, onComplete: (isSuccess: Boolean, error: Throwable?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid == userId) {
            val userDataList = mutableListOf<User>()
            val usersRef = database.reference.child("users")

            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val totalUsers = dataSnapshot.childrenCount
                    var fetchedUsers = 0L

                    for (userSnapshot in dataSnapshot.children) {
                        val user: User? = userSnapshot.getValue(User::class.java)

                        val userId = user?.userId
                        if (userId != null) {
                            val userRiffsRef = database.reference.child("riffs").orderByChild("userId").equalTo(userId)
                            userRiffsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(riffsSnapshot: DataSnapshot) {
                                    var totalGrade = 0f
                                    var gradeCount = 0

                                    for (riffSnapshot in riffsSnapshot.children) {
                                        val riff: Riff? = riffSnapshot.getValue(Riff::class.java)
                                        if (riff != null) {
                                            totalGrade += riff.avgGrade
                                            gradeCount++
                                        }
                                    }

                                    val avgGrade = if (gradeCount > 0) totalGrade / gradeCount else 0f
                                    user?.score = avgGrade

                                    userDataList.add(user!!)

                                    fetchedUsers++
                                    if (fetchedUsers == totalUsers) {
                                        _userDataList.value = userDataList
                                        onComplete(true, null)
                                    }
                                }

                                override fun onCancelled(riffsDatabaseError: DatabaseError) {
                                    onComplete(false, Throwable(riffsDatabaseError.message))
                                }
                            })
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    onComplete(false, Throwable(databaseError.message))
                }
            })
        }
    }
    /*fun fetchUserData(userId: String, onComplete: (isSuccess: Boolean, error: Throwable?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid == userId) {
            val userDataList = mutableListOf<User>()
            val usersRef = database.reference.child("users")

            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val totalUsers = dataSnapshot.childrenCount
                    var fetchedUsers = 0L

                    for (userSnapshot in dataSnapshot.children) {
                        val user: User? = userSnapshot.getValue(User::class.java)
                        userDataList.add(user!!)

                        fetchedUsers++
                        if (fetchedUsers == totalUsers) {
                            _userDataList.value = userDataList
                            onComplete(true, null)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    onComplete(false, Throwable(databaseError.message))
                }
            })
        }
    }*/

    fun registerUser(user: User, imageUri: Uri?, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, user.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser: FirebaseUser? = auth.currentUser
                    val userId = currentUser?.uid

                    if (userId != null) {
                        val storageRef = storage.reference.child("profilePictures").child("$userId.jpg")
                        val uploadTask = storageRef.putFile(imageUri!!)

                        uploadTask.addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                val newUser = User(
                                    userId,
                                    user.firstName,
                                    user.lastName,
                                    user.email,
                                    user.username,
                                    "",
                                    user.phoneNumber,
                                    downloadUri.toString(),
                                    user.score
                                )

                                usersRef.child(userId).setValue(newUser)
                                    .addOnSuccessListener {
                                        onComplete(true, null)
                                    }
                                    .addOnFailureListener { exception ->
                                        onComplete(false, exception.message)
                                    }
                            }.addOnFailureListener { exception ->
                                onComplete(false, exception.message)
                            }
                        }.addOnFailureListener { exception ->
                            onComplete(false, exception.message)
                        }
                    } else {
                        onComplete(false, "User ID is null")
                    }
                } else {
                    val errorMessage = when (val exception = task.exception as? FirebaseAuthException) {
                        is FirebaseAuthWeakPasswordException -> exception.reason
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email address."
                        is FirebaseAuthUserCollisionException -> "The email address is already in use."
                        else -> "Registration failed. Please try again later."
                    }
                    onComplete(false, errorMessage) // Registration failure
                }
            }
    }
    fun signInUser(email:String, password:String, onComplete: (Boolean, String?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        onComplete(true, userId, null)
                    } else {
                        onComplete(false, null, "User ID is null.")
                    }
                } else {
                    val errorMessage = when (val exception = task.exception) {
                        is FirebaseAuthInvalidUserException -> "Invalid username or password."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid username."
                        else -> "Sign-in failed. Please try again later."
                    }
                    onComplete(false, null, errorMessage)
                }
            }
    }

     private fun loadProfileImageFile(userId: String) {
        val storageRef = storage.reference.child("profilePictures").child("$userId.jpg")
        val localFile = createLocalImageFile()
        val downloadTask = storageRef.getFile(localFile)
        downloadTask.addOnSuccessListener { taskSnapshot ->
            _profileImageFile.postValue(localFile)
        }
        downloadTask.addOnFailureListener { exception ->
            exception.printStackTrace()
        }
    }

    private fun createLocalImageFile(): File {
        val fileName = "${System.currentTimeMillis()}_profile.jpg"
        val cacheDir = getApplication<Application>().cacheDir
        val imageFile = File(cacheDir, fileName)
        imageFile.createNewFile()
        return imageFile
    }

    fun saveUserData(userId: String, onComplete: (User?)-> Unit) {
        val userRef = usersRef.child(userId)
        loadProfileImageFile(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                _loggedInUser.value = user
                onComplete(user)
            }
            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
                onComplete(null)
            }
        })
    }
}