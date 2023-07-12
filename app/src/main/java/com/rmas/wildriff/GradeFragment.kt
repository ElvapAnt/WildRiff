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
import com.rmas.wildriff.databinding.FragmentGradeBinding
import com.rmas.wildriff.model.OtherRiffsViewModel
import com.rmas.wildriff.model.SelectedRiffViewModel
import com.rmas.wildriff.model.SharedViewModel
import com.rmas.wildriff.model.UserViewModel

class GradeFragment : Fragment() {
    private var _binding: FragmentGradeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private var userId: String? = null
    private lateinit var sharedViewModel: SharedViewModel

    private val otherRiffsViewModel: OtherRiffsViewModel by activityViewModels()
    private val selectedRiffViewModel: SelectedRiffViewModel by activityViewModels()

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

        _binding = FragmentGradeBinding.inflate(inflater, container, false)
        selectedRiffViewModel.selectedRiff.observe(viewLifecycleOwner){
        }
        userViewModel.saveUserData(sharedViewModel.userId.value!!) {
            println("Successfully saved the users data in PROFILE! : ${it?.username}")
        }

        otherRiffsViewModel.fetchOtherRiffs(sharedViewModel.userId.value!!) { success,error->
            if(success) {
                Toast.makeText(requireContext(), "Fetch success!", Toast.LENGTH_SHORT).show()
            } else{
                Toast.makeText(requireContext(), "Failed with $error", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ratingBar.rating = 0f
        binding.ratingBar.stepSize = 1f
        binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            Toast.makeText(requireContext(), "Rating : ${rating}", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmit.setOnClickListener {
            val selectedRiff = selectedRiffViewModel.selectedRiff.value
            val rating = binding.ratingBar.rating

            if (selectedRiff != null) {
                otherRiffsViewModel.updateRiffGrade(selectedRiff, rating) { isSuccess, error ->
                    if (isSuccess) {
                        Toast.makeText(requireContext(), "Grade submitted", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to submit grade: ${error?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        binding.btnCancel.setOnClickListener{
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}