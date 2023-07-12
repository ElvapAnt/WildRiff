package com.rmas.wildriff.model

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rmas.wildriff.data.Riff
import org.osmdroid.util.GeoPoint
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MyRiffsViewModel : ViewModel() {
    var MyRiffsList: ArrayList<Riff> = ArrayList()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance("https://wildriff-84a61-default-rtdb.europe-west1.firebasedatabase.app/")
    private val storage: FirebaseStorage =
        FirebaseStorage.getInstance("gs://wildriff-84a61.appspot.com")

    private val _myRiffsList: MutableLiveData<List<Riff>> = MutableLiveData()
    val myRiffsList: LiveData<List<Riff>> get() = _myRiffsList

    fun uploadRiff(
        riff: Riff,
        selectedAudioFile: Uri,
        onComplete: (isSuccess: Boolean, error: Throwable?) -> Unit
    ) {
        val riffRef: DatabaseReference = database.reference.child("riffs").push()
        val riffId = riffRef.key
        riff.riffId = riffId!!
        val userIdRiffsPath = "${riff.userId}_riffs"
        val storageRef: StorageReference = storage.reference.child(userIdRiffsPath).child(riffId!!)


        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid == riff.userId) {
            riffRef.setValue(riff).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uploadTask = storageRef.putFile(selectedAudioFile)

                    uploadTask.addOnSuccessListener {
                        MyRiffsList.add(riff)
                        _myRiffsList.value = MyRiffsList + listOf(riff)
                        onComplete(true, null)
                    }.addOnFailureListener { exception ->
                        onComplete(false, exception)
                    }
                } else {
                    onComplete(false, task.exception)
                }
            }
        }
        else {
            onComplete(false, Throwable("Permission denied"))
        }
    }

    fun fetchUserRiffs(userId: String, onComplete: (isSuccess: Boolean, error: Throwable?) -> Unit) {
        val userIdRiffsPath = "${userId}_riffs"
        val storageRef: StorageReference = storage.reference.child(userIdRiffsPath)
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid == userId) {
            storageRef.listAll()
                .addOnSuccessListener { listResult ->
                    val riffs: MutableList<Riff> = mutableListOf()

                    for (item in listResult.items) {
                        val riffRef: DatabaseReference =
                            database.reference.child("riffs").child(item.name)

                        riffRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val riff: Riff? = dataSnapshot.getValue(Riff::class.java)
                                riffs.add(riff!!)
                                _myRiffsList.value = riffs
                                onComplete(true,null)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                onComplete(false,error.toException())
                            }
                        })
                    }
                }
                .addOnFailureListener { exception ->
                    onComplete(false,Throwable(exception))
                }
        } else {
            onComplete(false,Throwable("Access denied!"))
        }
    }
    fun updateRiffLocation(riff: Riff, onComplete: (isSuccess: Boolean, error: Throwable?) -> Unit) {
        val riffRef = database.reference.child("riffs").child(riff.riffId!!)
        val updates = mapOf(
            "latitude" to riff.latitude,
            "longitude" to riff.longitude
        )
        riffRef.updateChildren(updates)
            .addOnSuccessListener {
                onComplete(true, null) // Success callback
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception) // Failure callback
            }
    }
}