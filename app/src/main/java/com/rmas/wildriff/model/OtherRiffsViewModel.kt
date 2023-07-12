package com.rmas.wildriff.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rmas.wildriff.data.Grade
import com.rmas.wildriff.data.Riff
import com.rmas.wildriff.data.User

class OtherRiffsViewModel: ViewModel() {
    var OtherRiffsList: ArrayList<Riff> = ArrayList()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance("https://wildriff-84a61-default-rtdb.europe-west1.firebasedatabase.app/")
    private val storage: FirebaseStorage =
        FirebaseStorage.getInstance("gs://wildriff-84a61.appspot.com")

    private val _otherRiffsList: MutableLiveData<List<Riff>> = MutableLiveData()
    val otherRiffsList: LiveData<List<Riff>> get() = _otherRiffsList

    fun fetchOtherRiffs(
        userId: String,
        onComplete: (isSuccess: Boolean, error: Throwable?) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            database.reference.child("users").get().addOnSuccessListener { dataSnapshot ->
                val userList: MutableList<User> = mutableListOf()

                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)

                    if (user?.userId != currentUser.uid) {
                        userList.add(user!!)
                    }
                }

                val riffs: MutableList<Riff> = mutableListOf()

                for (user in userList) {
                    val userIdRiffsPath = "${user.userId}_riffs"
                    val storageRef: StorageReference = storage.reference.child(userIdRiffsPath)

                    storageRef.listAll()
                        .addOnSuccessListener { listResult ->
                            val itemCount = listResult.items.size
                            var fetchedCount = 0

                            for (item in listResult.items) {
                                val riffRef: DatabaseReference =
                                    database.reference.child("riffs").child(item.name)

                                riffRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val riff: Riff? = dataSnapshot.getValue(Riff::class.java)
                                        riffs.add(riff!!)
                                        fetchedCount++
                                        if (fetchedCount == itemCount) {
                                            _otherRiffsList.value = riffs
                                            onComplete(true, null)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        fetchedCount++
                                        if (fetchedCount == itemCount) {
                                            onComplete(false, error.toException())
                                        }
                                    }
                                })
                            }
                            if (itemCount == 0) {
                                onComplete(true, null)
                            }
                        }
                        .addOnFailureListener { exception ->
                            onComplete(false, Throwable(exception))
                        }
                }
            }
                .addOnFailureListener { exception ->
                    onComplete(false, Throwable(exception))
                }
        } else {
            onComplete(false, Throwable("Access denied!"))
        }
    }

    fun updateRiffGrade(riff: Riff, grade: Float, onComplete: (isSuccess: Boolean, error: Throwable?) -> Unit) {
        val riffRef: DatabaseReference = database.reference.child("riffs").child(riff.riffId ?: "")
        val gradeData = Grade(grade, riff.riffId, riff.userId)
        riffRef.child("grades").orderByChild("userId").equalTo(riff.userId).limitToFirst(1).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    onComplete(false, Throwable("User has already graded this riff"))
                } else {
                    riffRef.child("grades").push().setValue(gradeData)
                        .addOnSuccessListener {
                            riffRef.child("grades").get().addOnSuccessListener { dataSnapshot ->
                                var totalGrade = 0f
                                var gradeCount = 0

                                for (gradeSnapshot in dataSnapshot.children) {
                                    val grade = gradeSnapshot.getValue(Grade::class.java)
                                    if (grade != null && grade.riffId == riff.riffId && grade.value != null) {
                                        totalGrade += grade.value!!
                                        gradeCount++
                                    }
                                }
                                val avgGrade = if (gradeCount > 0) totalGrade / gradeCount else 0f
                                riffRef.child("avgGrade").setValue(avgGrade)
                                    .addOnSuccessListener {
                                        onComplete(true, null)
                                    }
                                    .addOnFailureListener { exception ->
                                        onComplete(false, exception)
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            onComplete(false, exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception)
            }
    }
}