package com.rmas.wildriff

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.rmas.wildriff.data.User
import com.rmas.wildriff.databinding.FragmentRegisterBinding
import com.rmas.wildriff.model.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var userViewModel: UserViewModel
    private var selectedImageUri: Uri? = null
    private var localImageFile: File? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var profileImageView : ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        if (saveImageToFile()) {
                            profileImageView!!.setImageURI(selectedImageUri)
                            Toast.makeText(
                                requireContext(),
                                "Image saved locally!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "No image has been selected!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        handleImageSelectionCancel()
                    }
                }
            }
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    openGallery()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Gallery permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        profileImageView = binding.imageViewPfp

        val imageButtonSelectImage: ImageButton = binding.buttonSelectImage
        imageButtonSelectImage.setOnClickListener {
            checkGalleryPermission()
        }

        binding.buttonRegisterAcc.setOnClickListener {
            val firstName = binding.editTextFirstName.text.toString()
            val lastName = binding.editTextLastName.text.toString()
            val username = binding.editTextUsername.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()
            val phoneNumber = binding.editTextPhoneNumber.text.toString()
            var profileImageUrl:String = ""
            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(),"Please fill in all the fields",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(selectedImageUri == null){
                profileImageUrl = "default.jpg"
            }
            if (password != confirmPassword) {
                Toast.makeText(requireContext(),"Passwords don't match!",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val user = User("",firstName, lastName, email, username, password, phoneNumber, profileImageUrl, 0.0f)

            userViewModel.registerUser(user, selectedImageUri) { success, errorMessage ->
                if(success) {
                    Toast.makeText(requireContext(),"Registration successful!",Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_RegisterFragment_to_SignInFragment)
                } else {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val textView = binding.textViewSignUp
        val clickableText = getString(R.string.link_signin)
        val startIndex = 0
        val endIndex = startIndex + clickableText.length


        val spannableString = SpannableString(getString(R.string.link_signin))
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                findNavController().navigate(R.id.action_RegisterFragment_to_SignInFragment)
            }
        }
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.text = spannableString
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleImageSelectionCancel() {
        selectedImageUri = null
        profileImageView?.setImageResource(R.drawable.pick_pfp)
        Toast.makeText(requireContext(), "Image selection canceled", Toast.LENGTH_SHORT).show()
    }
    private fun checkGalleryPermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openGallery()
        } else {
            permissionLauncher.launch(permission)
        }
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }
    private fun createLocalImageFile(): File? {
        val fileName = "${System.currentTimeMillis()}_profile.jpg"
        val cacheDir = requireContext().cacheDir
        val imageFile = File(cacheDir, fileName)
        try {
            imageFile.createNewFile()
            return imageFile
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
    private fun saveImageToFile() : Boolean {
        if(selectedImageUri!=null) {
            localImageFile = createLocalImageFile()
            if (localImageFile != null) {
                val outputStream: OutputStream? = localImageFile?.outputStream()
                outputStream?.use { output ->
                    val input = requireContext().contentResolver.openInputStream(selectedImageUri!!)
                    input?.use { i ->
                        i.copyTo(output)
                    }
                }
                return true
            }
        }
        return false
    }

}