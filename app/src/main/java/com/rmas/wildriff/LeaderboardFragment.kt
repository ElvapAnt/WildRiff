package com.rmas.wildriff

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rmas.wildriff.adapter.LeaderboardAdapter
import com.rmas.wildriff.databinding.FragmentLeaderboardBinding
import com.rmas.wildriff.databinding.FragmentWelcomeBinding
import com.rmas.wildriff.model.MyRiffsViewModel
import com.rmas.wildriff.model.SharedViewModel
import com.rmas.wildriff.model.UserViewModel

class LeaderboardFragment : Fragment() {
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private var userId: String? = null

    private val userViewModel: UserViewModel by activityViewModels()
    private val myRiffsViewModel: MyRiffsViewModel by activityViewModels()
    private val leaderboardAdapter: LeaderboardAdapter by lazy { LeaderboardAdapter() }

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
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)

        userViewModel.saveUserData(sharedViewModel.userId.value!!) {
            println("Successfully saved the users data in LEADERBOARD! : ${it?.username}")
        }

        userViewModel.fetchUserData(sharedViewModel.userId.value!!) { success,error->
            if(success) {
                Toast.makeText(requireContext(), "Users successfully fetched!", Toast.LENGTH_SHORT).show()
            } else{
                Toast.makeText(requireContext(), "Failed with $error", Toast.LENGTH_SHORT).show()
            }
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerViewLeaderboard.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaderboardAdapter
        }
        userViewModel.userDataList.observe(viewLifecycleOwner) {
            leaderboardAdapter.submitList(userViewModel.userDataList.value!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}