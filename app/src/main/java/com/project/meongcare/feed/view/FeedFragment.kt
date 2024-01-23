package com.project.meongcare.feed.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.project.meongcare.R
import com.project.meongcare.databinding.FragmentFeedBinding
import com.project.meongcare.databinding.LayoutFeedNutrientBinding
import com.project.meongcare.feed.model.entities.FeedGetResponse
import com.project.meongcare.feed.viewmodel.DogViewModel
import com.project.meongcare.feed.viewmodel.FeedGetViewModel
import com.project.meongcare.feed.viewmodel.FeedPartGetViewModel
import com.project.meongcare.feed.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    val binding get() = _binding!!

    private val feedGetViewModel: FeedGetViewModel by viewModels()
    private val feedPartGetViewModel: FeedPartGetViewModel by viewModels()
    private val dogViewModel: DogViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private lateinit var feedPartAdapter: FeedPartAdapter
    private lateinit var feedGetResponse: FeedGetResponse

    private var dogId = 0L
    private var accessToken = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel.fetchAccessToken()
        dogViewModel.fetchDogId()
        dogViewModel.dogId.observe(viewLifecycleOwner) { response ->
            dogId = response
        }
        userViewModel.accessToken.observe(viewLifecycleOwner) { response ->
            accessToken = response
            feedGetViewModel.getFeed(
                accessToken,
                dogId,
            )
        }

        feedPartAdapter = FeedPartAdapter()
        feedGetViewModel.feedGet.observe(viewLifecycleOwner) { response ->
            feedGetResponse =
                FeedGetResponse(
                    response.brand,
                    response.feedName,
                    response.protein,
                    response.fat,
                    response.crudeAsh,
                    response.moisture,
                    response.days,
                    response.recommendIntake,
                    response.feedId,
                    response.feedRecordId,
                )

            if (feedGetResponse.brand == null) {
                binding.apply {
                    imageviewFeedBowlIllustration.visibility = View.VISIBLE
                    buttonFeedInputGuide.visibility = View.VISIBLE
                    textviewFeedBrand.visibility = View.GONE
                    textviewFeedName.visibility = View.GONE
                    piechartFeedNutrient.visibility = View.GONE
                    buttonFeedChange.visibility = View.GONE
                }
            } else {
                updateViewVisibilityBasedOnFeedExist(feedGetResponse.feedId)
                initFeedInfo(feedGetResponse.brand!!, feedGetResponse.feedName!!)
                initNutrientPieChart(
                    feedGetResponse.protein,
                    feedGetResponse.fat,
                    feedGetResponse.crudeAsh,
                    feedGetResponse.moisture,
                )
                initIntakePeriod(feedGetResponse.days)
                initDailyRecommendIntake(feedGetResponse.recommendIntake)
                updateViewVisibilityBasedOnOldFeedPartExist(
                    accessToken,
                    dogId,
                    feedGetResponse.feedRecordId,
                )
            }
            initNutrientTable(feedGetResponse)
            initOldFeedSeeMoreButton(feedGetResponse.feedRecordId)
        }

        initFeedAddButton()
        initOldFeedPartRecyclerView()
        initChangeButton()
    }

    private fun updateViewVisibilityBasedOnFeedExist(feedId: Long) {
        binding.apply {
            if (feedId == 0L) {
                textviewFeedBrand.visibility = View.GONE
                textviewFeedName.visibility = View.GONE
                piechartFeedNutrient.visibility = View.GONE
            } else {
                imageviewFeedBowlIllustration.visibility = View.GONE
                piechartFeedNutrient.visibility = View.VISIBLE
                buttonFeedInputGuide.visibility = View.GONE
            }
        }
    }

    private fun initFeedAddButton() {
        binding.buttonFeedInputGuide.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_feedAddFragment)
        }
    }

    private fun initFeedInfo(
        brand: String,
        feedName: String,
    ) {
        binding.textviewFeedBrand.text = brand
        binding.textviewFeedName.text = feedName
    }

    private fun initNutrientPieChart(
        protein: Double,
        fat: Double,
        crudeAsh: Double,
        moisture: Double,
    ) {
        val nutrientRatio =
            listOf(
                PieEntry(protein.toFloat()),
                PieEntry(fat.toFloat()),
                PieEntry(crudeAsh.toFloat()),
                PieEntry(moisture.toFloat()),
            )

        val pieColors =
            listOf(
                ContextCompat.getColor(requireContext(), R.color.main3),
                ContextCompat.getColor(requireContext(), R.color.sub7),
                ContextCompat.getColor(requireContext(), R.color.sub6),
                ContextCompat.getColor(requireContext(), R.color.sub8),
            )

        val dataSet = PieDataSet(nutrientRatio, "")
        dataSet.colors = pieColors
        dataSet.setDrawValues(false)

        binding.piechartFeedNutrient.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            isRotationEnabled = true
            holeRadius = 60f
            setTouchEnabled(false)
            animateY(1200, Easing.EaseInOutCubic)
            animate()
        }
    }

    private fun initNutrientTable(feedInfo: FeedGetResponse) {
        binding.run {
            initNutrientRow(
                includeFeedNutrientCrudeProtein,
                R.drawable.feed_rect_crude_protein_r5,
                "조단백",
                feedInfo.protein,
            )
            initNutrientRow(
                includeFeedNutrientCrudeFat,
                R.drawable.feed_rect_crude_fat_r5,
                "조지방",
                feedInfo.fat,
            )
            initNutrientRow(
                includeFeedNutrientCrudeAsh,
                R.drawable.feed_rect_crude_ash_r5,
                "조회분",
                feedInfo.crudeAsh,
            )
            initNutrientRow(
                includeFeedNutrientMoisture,
                R.drawable.feed_rect_moisture_r5,
                "수분",
                feedInfo.moisture,
            )
        }
    }

    private fun initNutrientRow(
        nutrientRow: LayoutFeedNutrientBinding,
        nutrientColorLabel: Int,
        nutrientType: String,
        nutrientPercentage: Double,
    ) {
        nutrientRow.apply {
            viewFeedNutrientColorLabel.setBackgroundResource(nutrientColorLabel)
            textviewFeedNutrientType.text = nutrientType
            textviewFeedNutrientPercentage.text = convertNutrientPercentage(nutrientPercentage.roundToInt())
        }
    }

    private fun initIntakePeriod(days: Long) {
        binding.textviewFeedIntakePeriodContent.text = convertIntakePeriod(days)
    }

    private fun initDailyRecommendIntake(recommendIntake: Int) {
        binding.textviewFeedDailyIntakeContent.text = convertDailyRecommendIntake(recommendIntake)
    }

    private fun updateViewVisibilityBasedOnOldFeedPartExist(
        accessToken: String,
        dogId: Long,
        feedRecordId: Long,
    ) {
        feedPartGetViewModel.getFeedPart(
            accessToken,
            dogId,
            feedRecordId,
        )
        feedPartGetViewModel.feedPartGet.observe(viewLifecycleOwner) { response ->
            feedPartAdapter.submitList(response.feedPartRecords)
        }
    }

    private fun initOldFeedSeeMoreButton(feedRecordId: Long) {
        binding.textviewFeedOldFeedSeeMore.setOnClickListener {
            val bundle = Bundle()
            bundle.putLong("feedRecordId", feedRecordId)
            findNavController().navigate(R.id.action_feedFragment_to_oldFeedFragment, bundle)
        }
    }

    private fun initOldFeedPartRecyclerView() {
        binding.recyclerviewFeedOldfeed.run {
            adapter = feedPartAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun initChangeButton() {
        binding.buttonFeedChange.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_searchFeedFragment)
        }
    }

    private fun convertNutrientPercentage(nutrientPercentage: Int) = String.format("%d%%", nutrientPercentage)

    private fun convertIntakePeriod(days: Long) = String.format("%d일", days)

    private fun convertDailyRecommendIntake(recommendIntake: Int) = String.format("%dg", recommendIntake)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}