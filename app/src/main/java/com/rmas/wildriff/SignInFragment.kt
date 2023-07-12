package com.rmas.wildriff

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.rmas.wildriff.databinding.FragmentSigninBinding
import com.rmas.wildriff.model.UserViewModel
import com.rmas.wildriff.data.User


class SignInFragment : Fragment() {

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!
    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        val intent = Intent(requireContext(), MainActivity::class.java)
        binding.buttonSignIn.setOnClickListener {
            val email = binding.editTextEmailSignIn.text.toString()
            val password = binding.editTextPasswordSignIn.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please fill in all the fields!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            userViewModel.signInUser(email,password) { success, userId, errorMessage ->
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        "Log-in successful! Starting Main activity...",
                        Toast.LENGTH_SHORT
                    ).show()
                    Intent(requireContext(), MainActivity::class.java).also {
                        it.putExtra("USER_ID",userId)
                        startActivity(it)
                    }
                } else {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val textView = binding.textViewSignUp
        val clickableText = getString(R.string.link_signup)
        val startIndex = 0
        val endIndex = startIndex + clickableText.length


        val spannableString = SpannableString(getString(R.string.link_signup))
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                findNavController().navigate(R.id.action_SignInFragment_to_RegisterFragment)
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
}