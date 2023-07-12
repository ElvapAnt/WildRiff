package com.rmas.wildriff

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import com.rmas.wildriff.data.Riff
import com.rmas.wildriff.databinding.FragmentAddRiffBinding
import com.rmas.wildriff.databinding.FragmentHomeBinding
import com.rmas.wildriff.model.MyRiffsViewModel
import com.rmas.wildriff.model.SharedViewModel
import com.rmas.wildriff.model.UserViewModel
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class AddRiffFragment : Fragment() {

    private var _binding: FragmentAddRiffBinding? = null
    private val binding get() = _binding!!
    private val myRiffsViewModel: MyRiffsViewModel by viewModels()
    private lateinit var selectFileLauncher: ActivityResultLauncher<String>
    private var mediaPlayer: MediaPlayer? = null
    private val userViewModel:UserViewModel by viewModels()
    private lateinit var sharedViewModel: SharedViewModel
    private var userId: String? = null

    private var selectedAudioUri: Uri? = null
    private var localAudioFile: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.userId.observe(this, Observer { id ->
            userId = id
        })

        selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedAudioUri = uri
                binding.textSelectedFile.text = "Selected File: ${getFileNameFromUri(uri)}"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddRiffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSelectFile.setOnClickListener {
            selectFileLauncher.launch("audio/*")
        }

        binding.buttonUpload.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val pitch = binding.editTextPitch.text.toString()
            val tonality = binding.editTextTonality.text.toString()
            val key = binding.editTextKey.text.toString()

            val riff = Riff(name, pitch, tonality, key, sharedViewModel.userId.value!!,"",0.0,0.0,0.0f, null)

            if(selectedAudioUri == null){
                Toast.makeText(requireContext(),"Please select an audio file!",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty() || pitch.isEmpty() || tonality.isEmpty() || key.isEmpty()) {
                Toast.makeText(requireContext(),"Please fill in all the fields",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            myRiffsViewModel.uploadRiff(riff, selectedAudioUri!!) { isSuccess, error ->
                if (isSuccess) {
                    Toast.makeText(
                        requireContext(),
                        "You've uploaded a new riff successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateBack()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "An error occured while trying to upload : ${error}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.buttonCancel.setOnClickListener {
            navigateBack()
        }

        binding.buttonPlay.setOnClickListener {
            try {
                if (mediaPlayer == null) {
                    MediaPlayer.create(requireContext(), selectedAudioUri).apply {
                        mediaPlayer = this
                    }
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

        binding.buttonReset.setOnClickListener {
            mediaPlayer?.seekTo(0)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }

    private fun navigateBack() {
        findNavController(requireView()).popBackStack()
    }


    @SuppressLint("Range")
    private fun getFileNameFromUri(uri: Uri): String {
        val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {

                val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                cursor.close()
                return displayName

            }
        }
        return ""
    }
}