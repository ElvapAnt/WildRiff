package com.rmas.wildriff

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.rmas.wildriff.databinding.FragmentPlayFilterBinding
import com.rmas.wildriff.model.CurrentLocationViewModel
import com.rmas.wildriff.model.OtherRiffsViewModel
import com.rmas.wildriff.model.SharedViewModel
import com.rmas.wildriff.model.UserViewModel

class PlayFilterFragment : Fragment() {
    private var _binding: FragmentPlayFilterBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private val otherRiffsViewModel: OtherRiffsViewModel by activityViewModels()
    private var userId: String? = null
    private lateinit var sharedViewModel: SharedViewModel

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private val currentLocationViewModel: CurrentLocationViewModel by activityViewModels()

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
    ): View {
        _binding = FragmentPlayFilterBinding.inflate(inflater, container, false)

        currentLocationViewModel.currLatitude.observe(viewLifecycleOwner) {
            currentLatitude = it
        }
        currentLocationViewModel.currLatitude.observe(viewLifecycleOwner) {
            currentLongitude = it
        }

        userViewModel.saveUserData(sharedViewModel.userId.value!!) {
            println("Successfully saved the users data in PROFILE! : ${it?.username}")
        }

        otherRiffsViewModel.fetchOtherRiffs(sharedViewModel.userId.value!!) { success, error ->
            if (success) {
                Toast.makeText(requireContext(), "Fetching success!!!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed with $error", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonApplyFilter.setOnClickListener {
            currentLocationViewModel.setLocation(
                currentLocationViewModel.currLatitude.value!!,
                currentLocationViewModel.currLongitude.value!!
            )

            val pitch = binding.editViewPitch.text.toString()
            val tonality = binding.editViewTonality.text.toString()
            val key = binding.editViewKey.text.toString()
            var radius = 0
            if (binding.editViewRadius.text.toString() != "") {
                radius = binding.editViewRadius.text.toString().toInt()
            }

            val args = Bundle().apply {
                putString("pitch", pitch)
                putString("tonality", tonality)
                putString("key", key)
                putInt("radius", radius)
            }

            findNavController().navigate(R.id.action_PlayFilterFragment_to_PlayFragment, args)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}