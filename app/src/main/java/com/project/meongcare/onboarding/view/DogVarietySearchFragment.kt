package com.project.meongcare.onboarding.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.meongcare.databinding.FragmentDogVarietySearchBinding
import com.project.meongcare.onboarding.model.data.local.DogTypeSelectListener
import com.project.meongcare.onboarding.viewmodel.DogTypeSharedViewModel
import com.project.meongcare.onboarding.viewmodel.DogTypeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DogVarietySearchFragment : Fragment(), DogTypeSelectListener {
    private lateinit var binding: FragmentDogVarietySearchBinding

    private val dogTypeViewModel: DogTypeViewModel by viewModels()
    private val dogTypeSharedViewModel: DogTypeSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentDogVarietySearchBinding.inflate(inflater)

        dogTypeViewModel.dogTypeList.observe(viewLifecycleOwner) { dogTypes ->
            if (dogTypes != null) {
                val adapter = binding.recyclerViewDogVariety.adapter as DogTypeAdapter
                adapter.updateDogTypeList(dogTypes)
            }
        }

        binding.run {
            imageViewBack.setOnClickListener {
                findNavController().popBackStack()
            }

            imageviewClearText.setOnClickListener {
                editTextDogVariety.setText("")
                clearFocus(editTextDogVariety)
            }

            recyclerViewDogVariety.run {
                adapter = DogTypeAdapter(inflater, this@DogVarietySearchFragment)
                layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }

            editTextDogVariety.doOnTextChanged { text, start, before, count ->
                val query = text.toString()
                dogTypeViewModel.searchDogType(query)
                imageviewClearText.visibility =
                    if (count > 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
        }

        return binding.root
    }

    fun clearFocus(view: View) {
        view.clearFocus()
        val inputMethodManager =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDogTypeSelected(dogType: String) {
        dogTypeSharedViewModel.setDogType(dogType)
        findNavController().navigateUp()
    }
}