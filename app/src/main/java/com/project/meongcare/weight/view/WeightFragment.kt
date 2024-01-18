package com.project.meongcare.weight.view

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.project.meongcare.R
import com.project.meongcare.databinding.FragmentWeightBinding
import com.project.meongcare.excreta.utils.SUCCESS
import com.project.meongcare.feed.viewmodel.DogViewModel
import com.project.meongcare.feed.viewmodel.UserViewModel
import com.project.meongcare.snackbar.view.CustomSnackBar
import com.project.meongcare.weight.model.entities.WeightGetRequest
import com.project.meongcare.weight.model.entities.WeightMonthResponse
import com.project.meongcare.weight.model.entities.WeightPatchRequest
import com.project.meongcare.weight.model.entities.WeightPostRequest
import com.project.meongcare.weight.model.entities.WeightWeeksResponse
import com.project.meongcare.weight.viewmodel.WeightViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.time.LocalDate
import kotlin.concurrent.thread
import kotlin.math.abs

@AndroidEntryPoint
class WeightFragment : Fragment() {
    private var _binding: FragmentWeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var inputMethodManager: InputMethodManager
    private val weightViewModel: WeightViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val dogViewModel: DogViewModel by viewModels()
    private var accessToken = ""
    private var dogId = 0L
    private var weight: Double? = null
    private lateinit var weightGetRequest: WeightGetRequest

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initInputMethodManager()
        userViewModel.fetchAccessToken()
        dogViewModel.fetchDogId()
        dogViewModel.dogId.observe(viewLifecycleOwner) { response ->
            dogId = response
            weightGetRequest =
                WeightGetRequest(
                    dogId,
                    "2024-01-19"
                )
        }
        userViewModel.accessToken.observe(viewLifecycleOwner) { response ->
            accessToken = response
            showWeightEditDialog()
            fetchWeeklyWeight()
            fetchMonthlyWeight()
            postWeight()
        }
    }

    private fun postWeight() {
        val weightPostRequest =
            WeightPostRequest(
                dogId,
                LocalDate.now().toString(),
                weight,
            )
        weightViewModel.postWeight(accessToken, weightPostRequest)
        weightViewModel.weightPosted.observe(viewLifecycleOwner) { response ->
            if (response == SUCCESS) {
                fetchDailyWeight()
                initWeightEditDialog()
            }
        }
    }

    private fun fetchDailyWeight() {
        weightViewModel.getDailyWeight(accessToken, weightGetRequest)
        weightViewModel.dayWeightGet.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                binding.textviewWeightRecordContent.text = response.weight.toString()
            }
        }
    }

    private fun fetchWeeklyWeight() {
        weightViewModel.getWeeklyWeight(accessToken, weightGetRequest)
        weightViewModel.weeklyWeightGet.observe(viewLifecycleOwner) { response ->
            initWeeklyRecordChart(response)
        }
    }

    private fun fetchMonthlyWeight() {
        weightViewModel.getMonthlyWeight(accessToken, weightGetRequest)
        weightViewModel.monthlyWeightGet.observe(viewLifecycleOwner) { response ->
            initMonthlyRecordChart(response)
            showMonthlyWeightVariation(response)
        }
    }

    private fun showMonthlyWeightVariation(response: WeightMonthResponse) {
        val monthlyWeightChange = (response.thisMonthWeight - response.lastMonthWeight)
        binding.run {
            when {
                monthlyWeightChange > 0 -> {
                    textviewWeightMonthlyrecordtitle.text = "이번달은 지난달 대비 증가했어요!"
                    textviewWeightMonthlyrecordcontent.text =
                        String.format("%.2f", monthlyWeightChange)
                    textviewWeightMonthlyRecordContentEnd.text = "kg가 증가했어요!"
                }
                monthlyWeightChange < 0 -> {
                    textviewWeightMonthlyrecordtitle.text = "이번달은 지난달 대비 감소했어요!"
                    textviewWeightMonthlyrecordcontent.text =
                        String.format("%.2f", abs(monthlyWeightChange))
                    textviewWeightMonthlyRecordContentEnd.text = "kg가 감소했어요!"
                }
                else -> {
                    textviewWeightMonthlyrecordtitle.text = "이번달과 지난달의 체중이 동일해요!"
                    textviewWeightMonthlyrecordcontent.text = ""
                    textviewWeightMonthlyRecordContentEnd.text = "변화가 없어요!"
                }
            }
        }
    }

    private fun showWeightEditDialog() {
        binding.run {
            textviewWeightEditbutton.setOnClickListener {
                layoutWeightEdit.root.visibility = View.VISIBLE
            }
        }
    }

    private fun initWeightEditDialog() {
        binding.layoutWeightEdit.run {
            buttonWeighteditdialogCancel.setOnClickListener { onCancelClicked() }
            buttonWeighteditdialogCheck.setOnClickListener { onCheckClicked() }
        }
    }

    private fun onCancelClicked() {
        hideSoftKeyboard()
        binding.layoutWeightEdit.run {
            edittextWeighteditdialog.text.clear()
            root.visibility = View.GONE
        }
    }

    private fun onCheckClicked() {
        val date = LocalDate.now().toString()
        val weightText = binding.layoutWeightEdit.edittextWeighteditdialog.text.toString()
        val weight = weightText.toDoubleOrNull() ?: return

        val weightPatchRequest =
            WeightPatchRequest(
                dogId,
                weight,
                date,
            )

        weightViewModel.patchWeight(accessToken, weightPatchRequest)
        weightViewModel.weightPatched.observe(viewLifecycleOwner) { response ->
            hideSoftKeyboard()
            binding.layoutWeightEdit.run {
                edittextWeighteditdialog.text.clear()
                root.visibility = View.GONE
            }
            if (response == SUCCESS) {
                fetchDailyWeight()
                fetchWeeklyWeight()
                fetchMonthlyWeight()
                CustomSnackBar.make(requireView(), R.drawable.snackbar_success_16dp, "체중이 수정되었습니다!").show()
            } else {
                CustomSnackBar.make(
                    requireView(),
                    R.drawable.snackbar_error_16dp,
                    "서버가 불안정 하여 체중 수정에 실패하였습니다.\n잠시 후 다시 시도해 주세요.",
                ).show()
            }
        }
    }

    private fun initWeeklyRecordChart(response: WeightWeeksResponse) {
        val weightWeeklyData = mutableListOf<Entry>()
        response.weeks.forEachIndexed { index, weightWeekResponse ->
            weightWeeklyData.add(Entry((index + 1).toFloat(), weightWeekResponse.weight.toFloat()))
        }

        val weightWeeklyDataSet = LineDataSet(weightWeeklyData, "")

        val lineColor = resources.getColor(R.color.main4, null)

        val typo = Typeface.createFromAsset(requireContext().assets, "pretendard_medium.otf")

        weightWeeklyDataSet.apply {
            valueTextSize = 12F
            valueTypeface = typo
            valueTextColor = resources.getColor(R.color.gray3, null)
            valueFormatter = WeightDataFormatter()
            color = lineColor
            setCircleColor(lineColor)
            setDrawCircleHole(false)
            setDrawFilled(true)
            fillDrawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.weight_weekly_chart_gradient)
        }

        binding.linechartWeightWeeklyrecord.apply {
            data = LineData(weightWeeklyDataSet)

            xAxis.apply {
                granularity = 1F
                textSize = 14F
                typeface = typo
                textColor = resources.getColor(R.color.gray4, null)
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = WeekFormatter()
                spaceMin = 0.2F
                spaceMax = 0.2F
                setDrawGridLines(false)
                axisLineColor = resources.getColor(R.color.white, null)
            }

            axisLeft.apply {
                setDrawLabels(false)
                setDrawAxisLine(false)
                gridColor = resources.getColor(R.color.gray2, null)
                gridLineWidth = 1F
            }

            axisRight.apply {
                setDrawLabels(false)
                setDrawAxisLine(false)
                setDrawGridLines(false)
            }

            description.isEnabled = false
            legend.xOffset = -50f
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawMarkers(true)
            marker = WeightCustomMarker(context, R.layout.weight_marker)
            animateY(1200)
        }
    }

    private fun initMonthlyRecordChart(response: WeightMonthResponse) {
        val weightMonthlyData =
            listOf(
                BarEntry(11F, response.lastMonthWeight.toFloat()),
                BarEntry(12F, response.thisMonthWeight.toFloat()),
            )

        val weightMonthlyDataSet = BarDataSet(weightMonthlyData, "")

        val barColors =
            listOf(
                resources.getColor(R.color.gray3, null),
                resources.getColor(R.color.main4, null),
            )

        val weightLabelColors =
            listOf(
                resources.getColor(R.color.gray5, null),
                resources.getColor(R.color.main4, null),
            )

        val typo = Typeface.createFromAsset(requireContext().assets, "pretendard_medium.otf")

        weightMonthlyDataSet.colors = barColors

        binding.barchartWeightMonthlyrecord.apply {
            data = BarData(weightMonthlyDataSet)

            data.apply {
                barWidth = 0.5F
                setValueTextSize(14F)
                setValueTypeface(typo)
                setValueTextColors(weightLabelColors)
                setValueFormatter(WeightDataFormatter())
            }

            xAxis.apply {
                granularity = 1F
                position = XAxis.XAxisPosition.BOTTOM
                textSize = 14F
                typeface = typo
                valueFormatter = MonthFormatter()
                setDrawGridLines(false)
            }

            axisLeft.apply {
                axisMinimum = 0F
                granularity = 1F
                setDrawLabels(false)
                setDrawAxisLine(false)
                gridColor = resources.getColor(R.color.gray2, null)
                gridLineWidth = 1F
            }

            axisRight.apply {
                setDrawLabels(false)
                setDrawAxisLine(false)
                setDrawGridLines(false)
            }

            description.isEnabled = false
            legend.xOffset = -50f
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            animateY(1000)
        }
    }

    class WeekFormatter : ValueFormatter() {
        private val format = DecimalFormat("#주")

        override fun getFormattedValue(value: Float): String {
            return format.format(value)
        }
    }

    class MonthFormatter : ValueFormatter() {
        private val format = DecimalFormat("#월")

        override fun getFormattedValue(value: Float): String {
            return format.format(value)
        }
    }

    class WeightDataFormatter : ValueFormatter() {
        private val format = DecimalFormat("0.00kg")

        override fun getFormattedValue(value: Float): String {
            return format.format(value)
        }
    }

    private fun initInputMethodManager() {
        thread {
            SystemClock.sleep(1000)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
