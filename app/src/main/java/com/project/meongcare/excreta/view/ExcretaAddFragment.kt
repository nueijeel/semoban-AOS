package com.project.meongcare.excreta.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.project.meongcare.CalendarBottomSheetFragment
import com.project.meongcare.R
import com.project.meongcare.databinding.FragmentExcretaAddEditBinding
import com.project.meongcare.excreta.model.entities.Excreta
import com.project.meongcare.excreta.utils.ExcretaDateTimeUtils.convertDateFormat
import com.project.meongcare.excreta.utils.ExcretaDateTimeUtils.convertTimeFormat
import com.project.meongcare.excreta.utils.ExcretaDateTimeUtils.plusDay
import com.project.meongcare.excreta.utils.SUCCESS
import com.project.meongcare.excreta.viewmodel.ExcretaAddViewModel
import com.project.meongcare.onboarding.model.data.local.DateSubmitListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExcretaAddFragment : Fragment(), DateSubmitListener {
    private var _binding: FragmentExcretaAddEditBinding? = null
    private val binding get() = _binding!!

    private val excretaAddViewModel: ExcretaAddViewModel by viewModels()
    private var excretaDate = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExcretaAddEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        initPhotoAttachModalBottomSheet()
        initCalendarModalBottomSheet()
        observeAndUpdateExcretaDate()
        toggleExcretaCheckboxesOnClick()
        saveExcretaInfo()
    }

    private fun initToolbar() {
        binding.toolbarExcretaadd.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initPhotoAttachModalBottomSheet() {
        val photoAttachModalBottomSheet = PhotoAttachModalBottomSheetFragment()
        binding.cardviewExcretaaddImage.setOnClickListener {
            photoAttachModalBottomSheet.show(
                requireActivity().supportFragmentManager, PhotoAttachModalBottomSheetFragment.TAG
            )
        }
    }

    private fun initCalendarModalBottomSheet() {
        binding.textviewExcretaaddDate.setOnClickListener {
            val calendarModalBottomSheet = CalendarBottomSheetFragment()
            calendarModalBottomSheet.setDateSubmitListener(this@ExcretaAddFragment)
            calendarModalBottomSheet.show(
                requireActivity().supportFragmentManager, calendarModalBottomSheet.tag
            )
        }
    }

    private fun observeAndUpdateExcretaDate(): String {
        excretaAddViewModel.excretaDate.observe(viewLifecycleOwner) { date ->
            if (date != null) {
                excretaDate = plusDay(date)
                binding.textviewExcretaaddDate.run {
                    setTextColor(resources.getColor(R.color.black, null))
                    setTextAppearance(R.style.Typography_Body1_Medium)
                    text = convertDateFormat(date)
                }
            }
        }
        return excretaDate
    }

    private fun toggleExcretaCheckboxesOnClick() {
        binding.run {
            checkboxExcretaaddFeces.setOnClickListener {
                checkboxExcretaaddUrine.isChecked = !checkboxExcretaaddFeces.isChecked
            }
            checkboxExcretaaddUrine.setOnClickListener {
                checkboxExcretaaddFeces.isChecked = !checkboxExcretaaddUrine.isChecked
            }
        }
    }

    override fun onDateSubmit(str: String) {
        excretaAddViewModel.getExcretaDate(str)
    }

    private fun saveExcretaInfo() {
        binding.apply {
            buttonExcretaaddCompletion.setOnClickListener {
                val excretaType = if (checkboxExcretaaddUrine.isChecked) Excreta.URINE.toString()
                else Excreta.FECES.toString()

                val excretaTime = convertTimeFormat(timepikerExcretaaddTime)
                val excretaDateTime = "${excretaDate}T${excretaTime}"

                excretaAddViewModel.postExcreta(excretaType, excretaDateTime)
                excretaAddViewModel.excretaPosted.observe(viewLifecycleOwner) { response ->
                    if (response == SUCCESS) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
