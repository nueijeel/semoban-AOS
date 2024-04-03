package com.project.meongcare.feed.view

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.archit.calendardaterangepicker.customviews.CalendarListener
import com.archit.calendardaterangepicker.customviews.DateRangeCalendarView
import com.bumptech.glide.Glide
import com.project.meongcare.BuildConfig
import com.project.meongcare.R
import com.project.meongcare.aws.util.AWSS3ImageUtils.convertUriToFile
import com.project.meongcare.aws.util.FEED_FOLDER_PATH
import com.project.meongcare.aws.util.PARENT_FOLDER_PATH
import com.project.meongcare.aws.viewmodel.AWSS3ViewModel
import com.project.meongcare.databinding.FragmentFeedAddEditBinding
import com.project.meongcare.excreta.utils.SUCCESS
import com.project.meongcare.feed.model.data.local.FeedPhotoListener
import com.project.meongcare.feed.model.entities.FeedPostRequest
import com.project.meongcare.feed.model.entities.FeedUploadRequest
import com.project.meongcare.feed.model.utils.END_DATE
import com.project.meongcare.feed.model.utils.FEED_POST_FAILURE
import com.project.meongcare.feed.model.utils.FEED_POST_SUCCESS
import com.project.meongcare.feed.model.utils.FeedDateUtils.convertDateFormat
import com.project.meongcare.feed.model.utils.FeedInfoUtils.calculateRecommendDailyIntake
import com.project.meongcare.feed.model.utils.FeedInfoUtils.convertFeedFile
import com.project.meongcare.feed.model.utils.FeedInfoUtils.convertFeedPostDto
import com.project.meongcare.feed.model.utils.FeedInfoUtils.initRecommendDailyIntake
import com.project.meongcare.feed.model.utils.FeedInfoUtils.showFailureSnackBar
import com.project.meongcare.feed.model.utils.FeedInfoUtils.showSuccessSnackBar
import com.project.meongcare.feed.model.utils.FeedValidationUtils.validationBrandAndFeedName
import com.project.meongcare.feed.model.utils.FeedValidationUtils.validationIngredient
import com.project.meongcare.feed.model.utils.FeedValidationUtils.validationIntakePeriod
import com.project.meongcare.feed.model.utils.FeedValidationUtils.validationKcal
import com.project.meongcare.feed.model.utils.FeedValidationUtils.validationTotalIngredient
import com.project.meongcare.feed.model.utils.INTAKE_PERIOD_ERROR
import com.project.meongcare.feed.model.utils.START_DATE
import com.project.meongcare.feed.viewmodel.DogViewModel
import com.project.meongcare.feed.viewmodel.FeedPostViewModel
import com.project.meongcare.feed.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

@AndroidEntryPoint
class FeedAddFragment : Fragment(), FeedPhotoListener {
    private var _binding: FragmentFeedAddEditBinding? = null
    val binding get() = _binding!!

    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var imageFile: File
    private lateinit var filePath: String

    private val feedPostViewModel: FeedPostViewModel by viewModels()
    private val dogViewModel: DogViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val awsS3ViewModel: AWSS3ViewModel by viewModels()

    private var recommendIntake = 0.0
    var selectedStartDate = ""
    private var selectedEndDate: String? = null
    private var imageUri: Uri? = null

    private var proteinValue = 0.0
    private var fatValue = 0.0
    private var ashValue = 0.0
    private var moistureValue = 0.0
    private var etcValue = 0.0
    private var kcal = ""
    private var weight = 0.0

    private var dogId = 0L
    private var accessToken = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFeedAddEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        dogViewModel.fetchDogId()
        dogViewModel.dogId.observe(viewLifecycleOwner) { response ->
            dogId = response
        }
        userViewModel.fetchAccessToken()
        userViewModel.accessToken.observe(viewLifecycleOwner) { response ->
            accessToken = response
        }
        dogViewModel.fetchDogWeight()
        dogViewModel.dogWeight.observe(viewLifecycleOwner) { response ->
            weight = response
        }
        awsS3ViewModel.uploadImageResponse.observe(viewLifecycleOwner) { response ->
            if (response == 200) {
                val imageURL = BuildConfig.AWS_S3_BASE_URL + filePath
                postFeedInfo(imageURL)
            }
        }
        initInputMethodManager()
        initToolbar()
        initPhotoAttachModalBottomSheet()
        applyKeyboardHidingAction()
        updateEtcPercentage()
        applyKcalContentEditorBehavior()
        updateCalendarVisibility()
        updateSelectedIntakePeriod()
        validationFeedInfo()
    }

    private fun applyKcalContentEditorBehavior() {
        binding.apply {
            edittextFeedaddeditKcalContent.apply {
                doAfterTextChanged {
                    recommendIntake =
                        calculateRecommendDailyIntake(
                            weight,
                            text.toString().toDoubleOrNull() ?: 0.0,
                        )
                    initRecommendDailyIntake(
                        recommendIntake,
                        textviewFeedaddeditDailyIntakeContent,
                    )
                }
            }
        }
    }

    private fun initToolbar() {
        binding.toolbarFeedaddedit.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initPhotoAttachModalBottomSheet() {
        binding.cardviewFeedaddeditImage.setOnClickListener {
            val photoAttachModalBottomSheet = FeedPhotoAttachModalBottomSheetFragment()
            photoAttachModalBottomSheet.setPhotoListener(this@FeedAddFragment)
            photoAttachModalBottomSheet.show(
                requireActivity().supportFragmentManager,
                FeedPhotoAttachModalBottomSheetFragment.TAG,
            )
        }
    }

    private fun hideKeyboardOnAction(editText: EditText) {
        editText.apply {
            setOnEditorActionListener { _, _, _ ->
                hideSoftKeyboard()
                true
            }
        }
    }

    private fun applyKeyboardHidingAction() {
        binding.apply {
            hideKeyboardOnAction(edittextFeedaddeditBrand)
            hideKeyboardOnAction(edittextFeedaddeditName)
            hideKeyboardOnAction(edittextFeedaddeditCrudeProteinPercentage)
            hideKeyboardOnAction(edittextFeedaddeditCrudeFatPercent)
            hideKeyboardOnAction(edittextFeedaddeditCrudeAshPercent)
            hideKeyboardOnAction(edittextFeedaddeditMoisturePercent)
        }
    }

    private fun calculateEtcPercent(editText: EditText) {
        binding.apply {
            editText.doAfterTextChanged {
                val protein = edittextFeedaddeditCrudeProteinPercentage.text.toString().toDoubleOrNull() ?: 0.0
                val fat = edittextFeedaddeditCrudeFatPercent.text.toString().toDoubleOrNull() ?: 0.0
                val ash = edittextFeedaddeditCrudeAshPercent.text.toString().toDoubleOrNull() ?: 0.0
                val moisture = edittextFeedaddeditMoisturePercent.text.toString().toDoubleOrNull() ?: 0.0
                val etc = 100.0 - protein - fat - ash - moisture

                if (etc in 0.0..100.0) {
                    textviewFeedaddeditEtcPercent.text = etc.toString()
                } else {
                    validationTotalIngredient(
                        textviewFeedaddeditIngredientAndKcalError,
                        scrollviewFeedadd,
                        textviewFeedaddeditKcalTitle,
                    )
                    editText.setText("")
                }
            }
        }
    }

    private fun updateEtcPercentage() {
        binding.apply {
            calculateEtcPercent(edittextFeedaddeditCrudeProteinPercentage)
            calculateEtcPercent(edittextFeedaddeditCrudeFatPercent)
            calculateEtcPercent(edittextFeedaddeditCrudeAshPercent)
            calculateEtcPercent(edittextFeedaddeditMoisturePercent)
        }
    }

    private fun updateCalendarVisibility() {
        binding.apply {
            textviewFeedaddeditIntakePeriodStart.apply {
                setOnClickListener {
                    setBackgroundResource(R.drawable.all_rect_gray1_r5)
                    setTextColor(resources.getColor(R.color.black, null))
                    calendarviewFeedaddeditStartDate.visibility = View.VISIBLE
                    calendarviewFeedaddeditEndDate.visibility = View.GONE
                    checkboxFeedaddeditDoNotKnowEndDate.visibility = View.GONE
                    textviewFeedaddeditDoNotKnowEndDate.visibility = View.GONE
                    textviewFeedaddeditIntakePeriodEnd.setTextColor(resources.getColor(R.color.gray4, null))
                    scrollviewFeedadd.smoothScrollTo(0, buttonFeedaddeditCompletion.bottom)
                }
            }
            textviewFeedaddeditIntakePeriodEnd.apply {
                setOnClickListener {
                    setBackgroundResource(R.drawable.all_rect_gray1_r5)
                    setTextColor(resources.getColor(R.color.black, null))
                    calendarviewFeedaddeditEndDate.visibility = View.VISIBLE
                    calendarviewFeedaddeditStartDate.visibility = View.INVISIBLE
                    checkboxFeedaddeditDoNotKnowEndDate.visibility = View.VISIBLE
                    textviewFeedaddeditDoNotKnowEndDate.visibility = View.VISIBLE
                    textviewFeedaddeditIntakePeriodStart.setTextColor(resources.getColor(R.color.gray4, null))
                    scrollviewFeedadd.smoothScrollTo(0, buttonFeedaddeditCompletion.bottom)
                }
            }
        }
    }

    private fun updateSelectedIntakePeriodStartDate(
        calendar: DateRangeCalendarView,
        date: TextView,
    ) {
        calendar.setCalendarListener(
            object : CalendarListener {
                override fun onDateRangeSelected(
                    startDate: Calendar,
                    endDate: Calendar,
                ) {
                    calendar.resetAllSelectedViews()
                }

                override fun onFirstDateSelected(startDate: Calendar) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    date.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    selectedStartDate = dateFormat.format(startDate.time)

                    date.text = convertDateFormat(selectedStartDate)
                }
            },
        )
    }

    private fun updateSelectedIntakePeriodEndDate(
        calendar: DateRangeCalendarView,
        date: TextView,
        checkBox: CheckBox,
    ) {
        calendar.setCalendarListener(
            object : CalendarListener {
                override fun onDateRangeSelected(
                    startDate: Calendar,
                    endDate: Calendar,
                ) {
                    calendar.resetAllSelectedViews()
                }

                override fun onFirstDateSelected(startDate: Calendar) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    date.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    selectedEndDate = dateFormat.format(startDate.time)
                    date.text = convertDateFormat(selectedEndDate)

                    checkBox.setOnClickListener {
                        calendar.resetAllSelectedViews()
                        selectedEndDate = null
                        date.text = "모름"
                    }

                    if (date.text != "모름") {
                        checkBox.isChecked = false
                    }
                }
            },
        )
    }

    private fun updateSelectedIntakePeriod() {
        binding.apply {
            updateSelectedIntakePeriodStartDate(
                calendarviewFeedaddeditStartDate,
                textviewFeedaddeditIntakePeriodStart,
            )
            updateSelectedIntakePeriodEndDate(
                calendarviewFeedaddeditEndDate,
                textviewFeedaddeditIntakePeriodEnd,
                checkboxFeedaddeditDoNotKnowEndDate,
            )
            checkboxFeedaddeditDoNotKnowEndDate.setOnClickListener {
                textviewFeedaddeditIntakePeriodEnd.text = "모름"
            }
        }
    }

    private fun createFeedInfo(imageURL: String?): FeedPostRequest {
        binding.apply {
            val brand = edittextFeedaddeditBrand.text.toString()
            val feedName = edittextFeedaddeditName.text.toString()
            val feedPostRequest =
                FeedPostRequest(
                    dogId,
                    brand,
                    feedName,
                    proteinValue,
                    fatValue,
                    ashValue,
                    moistureValue,
                    etcValue,
                    kcal.toDouble(),
                    recommendIntake.toInt(),
                    selectedStartDate,
                    selectedEndDate,
                    imageURL,
                )
            return feedPostRequest
        }
    }

    private fun validationFeedInfo() {
        binding.apply {
            buttonFeedaddeditCompletion.setOnClickListener {
                var isValid = true

                if (selectedEndDate != null) {
                    val startDate = selectedStartDate.replace("-", "").toInt()
                    val endDate = selectedEndDate?.replace("-", "")?.toInt()!!

                    if (startDate > endDate) {
                        textviewFeedaddeditIntakePeriodError.apply {
                            text = INTAKE_PERIOD_ERROR
                            visibility = View.VISIBLE
                        }
                        isValid = false
                    }
                }

                if (textviewFeedaddeditIntakePeriodEnd.text == END_DATE) {
                    validationIntakePeriod(
                        textviewFeedaddeditIntakePeriodEnd,
                        textviewFeedaddeditIntakePeriodError,
                    )
                    isValid = false
                }

                if (textviewFeedaddeditIntakePeriodStart.text == START_DATE) {
                    validationIntakePeriod(
                        textviewFeedaddeditIntakePeriodStart,
                        textviewFeedaddeditIntakePeriodError,
                    )
                    isValid = false
                }

                kcal = edittextFeedaddeditKcalContent.text.toString()
                if (kcal.isEmpty()) {
                    validationKcal(
                        edittextFeedaddeditKcalContent,
                        textviewFeedaddeditIngredientAndKcalError,
                    )
                    isValid = false
                }

                val protein = edittextFeedaddeditCrudeProteinPercentage.text.toString()
                val fat = edittextFeedaddeditCrudeFatPercent.text.toString()
                val ash = edittextFeedaddeditCrudeAshPercent.text.toString()
                val moisture = edittextFeedaddeditMoisturePercent.text.toString()
                val etc = textviewFeedaddeditEtcPercent.text.toString()

                proteinValue = protein.toDoubleOrNull() ?: 0.0
                fatValue = fat.toDoubleOrNull() ?: 0.0
                ashValue = ash.toDoubleOrNull() ?: 0.0
                moistureValue = moisture.toDoubleOrNull() ?: 0.0
                etcValue = etc.toDoubleOrNull() ?: 0.0

                if (protein.isEmpty()) {
                    validationIngredient(
                        textviewFeedaddeditIngredientAndKcalError,
                        edittextFeedaddeditCrudeProteinPercentage,
                        scrollviewFeedadd,
                        textviewFeedaddeditIngredient,
                    )
                    isValid = false
                }

                if (fat.isEmpty()) {
                    validationIngredient(
                        textviewFeedaddeditIngredientAndKcalError,
                        edittextFeedaddeditCrudeFatPercent,
                        scrollviewFeedadd,
                        textviewFeedaddeditIngredient,
                    )
                    isValid = false
                }

                if (ash.isEmpty()) {
                    validationIngredient(
                        textviewFeedaddeditIngredientAndKcalError,
                        edittextFeedaddeditCrudeAshPercent,
                        scrollviewFeedadd,
                        textviewFeedaddeditIngredient,
                    )
                    isValid = false
                }

                if (moisture.isEmpty()) {
                    validationIngredient(
                        textviewFeedaddeditIngredientAndKcalError,
                        edittextFeedaddeditMoisturePercent,
                        scrollviewFeedadd,
                        textviewFeedaddeditIngredient,
                    )
                    isValid = false
                }

                val totalIngredient = proteinValue + fatValue + ashValue + moistureValue + etcValue

                if (totalIngredient > 100) {
                    validationTotalIngredient(
                        textviewFeedaddeditIngredientAndKcalError,
                        scrollviewFeedadd,
                        textviewFeedaddeditIngredient,
                    )
                    isValid = false
                }

                if (edittextFeedaddeditName.text.toString().isEmpty()) {
                    validationBrandAndFeedName(
                        edittextFeedaddeditName,
                        textviewFeedaddeditNameError,
                        scrollviewFeedadd,
                        textviewFeedaddeditName,
                        inputMethodManager,
                    )
                    isValid = false
                }

                if (edittextFeedaddeditBrand.text.toString().isEmpty()) {
                    validationBrandAndFeedName(
                        edittextFeedaddeditBrand,
                        textviewFeedaddeditBrandError,
                        scrollviewFeedadd,
                        textviewFeedaddeditBrand,
                        inputMethodManager,
                    )
                    isValid = false
                }

                if (isValid) {
                    val uri = feedPostViewModel.feedImage.value
                    if (uri == null) {

                    } else {
                        getPreSignedUrl(uri)
                    }
                }
            }
        }
    }

    private fun postFeedInfo(imageURL: String?) {
        feedPostViewModel.postFeed(
            accessToken,
            createFeedInfo(imageURL),
        )
        feedPostViewModel.feedPosted.observe(viewLifecycleOwner) { response ->
            if (response == SUCCESS) {
                findNavController().popBackStack()
                showSuccessSnackBar(
                    requireView(),
                    FEED_POST_SUCCESS,
                )
            } else {
                showFailureSnackBar(
                    requireView(),
                    FEED_POST_FAILURE,
                )
            }
        }
    }

    private fun getPreSignedUrl(uri: Uri) {
        imageFile = convertUriToFile(requireContext(), uri)
        filePath = "$PARENT_FOLDER_PATH$FEED_FOLDER_PATH${imageFile.name}"
        awsS3ViewModel.getPreSignedUrl(accessToken, filePath)
        awsS3ViewModel.preSignedUrl.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                awsS3ViewModel.uploadImageToS3(response.preSignedUrl, requestBody)
            }
        }
    }

    private fun initInputMethodManager() {
        thread {
            SystemClock.sleep(300)
            inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            hideSoftKeyboard()
        }
    }

    private fun hideSoftKeyboard() {
        if (requireActivity().currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus!!.windowToken, 0)
            requireActivity().currentFocus!!.clearFocus()
        }
    }

    override fun onUriPassed(uri: Uri) {
        feedPostViewModel.getFeedImage(uri)
        binding.apply {
            Glide.with(this@FeedAddFragment)
                .load(uri)
                .into(imageviewFeedaddeditPicture)
            layoutFeedaddeditImage.root.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
