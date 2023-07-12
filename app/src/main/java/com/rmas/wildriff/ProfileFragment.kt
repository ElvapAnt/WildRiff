package com.rmas.wildriff

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rmas.wildriff.adapter.RiffClickListener
import com.rmas.wildriff.adapter.RiffsAdapter
import com.rmas.wildriff.data.Riff
import com.rmas.wildriff.data.User
import com.rmas.wildriff.databinding.FragmentProfileBinding
import com.rmas.wildriff.model.MyRiffsViewModel
import com.rmas.wildriff.model.SharedViewModel
import com.rmas.wildriff.model.UserViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class ProfileFragment:Fragment(), RiffClickListener{
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var riffsAdapter: RiffsAdapter
    private val riffsList: ArrayList<Riff> = ArrayList()
    private var mediaPlayer: MediaPlayer? = null


    private val userViewModel: UserViewModel by activityViewModels()
    private val myRiffsViewModel: MyRiffsViewModel by activityViewModels()
    private var userId: String? = null
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.userId.observe(this, Observer { id ->
            userId = id
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        riffsAdapter = RiffsAdapter(myRiffsViewModel.MyRiffsList, this)
        binding.recyclerViewRiffs.adapter = riffsAdapter
        binding.recyclerViewRiffs.layoutManager = LinearLayoutManager(requireContext())

        userViewModel.loggedInUser.observe(viewLifecycleOwner) {
            binding.textViewUsername.setText(it?.username)
        }

        userViewModel.profileImageFile.observe(viewLifecycleOwner){
            if (it != null) {
                Glide.with(requireContext())
                    .load(it)
                    .placeholder(R.drawable.pick_pfp)
                    .error(R.drawable.pick_pfp)
                    .centerCrop()
                    .into(binding.imageProfile)
            }
        }

        myRiffsViewModel.myRiffsList.observe(viewLifecycleOwner) {
            val previousSize = riffsAdapter.itemCount

            riffsList.clear()
            riffsList.addAll(myRiffsViewModel.myRiffsList.value!!)

            if (previousSize < riffsList.size) {
                riffsAdapter.notifyItemRangeInserted(previousSize, riffsList.size - previousSize)
            } else if (previousSize > riffsList.size) {
                riffsAdapter.notifyItemRangeRemoved(riffsList.size, previousSize - riffsList.size)
            }
        }

        userViewModel.saveUserData(sharedViewModel.userId.value!!) {
            println("Successfully saved the users data in PROFILE! : ${it?.username}")
        }

        myRiffsViewModel.fetchUserRiffs(sharedViewModel.userId.value!!) { success,error->
            if(success) {
                riffsAdapter = RiffsAdapter(myRiffsViewModel.myRiffsList.value?: emptyList(),this)
                binding.recyclerViewRiffs.adapter = riffsAdapter
                binding.recyclerViewRiffs.layoutManager = LinearLayoutManager(requireContext())
            } else{
                Toast.makeText(requireContext(), "Failed with $error", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonAddRiff.setOnClickListener{
            findNavController().navigate(R.id.action_ProfileFragment_to_AddRiffFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }

    override fun onRiffClick(riff: Riff) {
        if(mediaPlayer!=null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        val riffId = riff.riffId
        val userIdRiffsPath = "${riff.userId}_riffs"
        val storageRef: StorageReference = FirebaseStorage.getInstance("gs://wildriff-84a61.appspot.com")
            .reference.child(userIdRiffsPath).child(riffId!!)

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                try {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(requireContext(), uri)
                        }
                        mediaPlayer?.prepare()
                        mediaPlayer?.start()
                    } else if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    } else {
                        mediaPlayer?.start()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Failed to play file : $exception",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }
}