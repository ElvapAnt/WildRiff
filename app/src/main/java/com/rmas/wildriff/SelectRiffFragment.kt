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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rmas.wildriff.adapter.RiffClickListener
import com.rmas.wildriff.adapter.RiffsAdapter
import com.rmas.wildriff.data.Riff
import com.rmas.wildriff.databinding.FragmentSelectRiffBinding
import com.rmas.wildriff.model.LocationViewModel
import com.rmas.wildriff.model.MyRiffsViewModel
import com.rmas.wildriff.model.SharedViewModel
import com.rmas.wildriff.model.UserViewModel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class SelectRiffFragment : Fragment(), RiffClickListener {
    private var _binding: FragmentSelectRiffBinding? = null
    private val binding get() = _binding!!

    private lateinit var riffsAdapter: RiffsAdapter
    private val riffsList: ArrayList<Riff> = ArrayList()

    private val userViewModel: UserViewModel by activityViewModels()
    private val myRiffsViewModel: MyRiffsViewModel by activityViewModels()
    private var userId: String? = null
    private lateinit var sharedViewModel: SharedViewModel
    private val locationViewModel : LocationViewModel by activityViewModels()

    private var clickedLocation: GeoPoint? = null

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
        _binding = FragmentSelectRiffBinding.inflate(inflater, container, false)


        locationViewModel.latitude.observe(viewLifecycleOwner){
            clickedLocation?.latitude = it
        }
        locationViewModel.longitude.observe(viewLifecycleOwner){
            clickedLocation?.longitude = it
        }

        riffsAdapter = RiffsAdapter(myRiffsViewModel.MyRiffsList, this)
        binding.recyclerViewRiffs.adapter = riffsAdapter
        binding.recyclerViewRiffs.layoutManager = LinearLayoutManager(requireContext())

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRiffClick(riff: Riff) {
        riff.longitude = locationViewModel.longitude.value!!
        riff.latitude = locationViewModel.latitude.value!!
        myRiffsViewModel.updateRiffLocation(riff) { isSuccess, error ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Successfully added the riff to your location!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Failed to update riff location: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
