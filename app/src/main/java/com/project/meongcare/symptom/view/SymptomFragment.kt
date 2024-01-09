package com.project.meongcare.symptom.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.meongcare.MainActivity
import com.project.meongcare.R
import com.project.meongcare.databinding.FragmentSymptomBinding
import com.project.meongcare.databinding.ItemSymptomBinding
import com.project.meongcare.symptom.utils.SymptomUtils.Companion.convertDateToTime
import com.project.meongcare.symptom.utils.SymptomUtils.Companion.getSymptomImg
import com.project.meongcare.symptom.viewmodel.SymptomViewModel
import com.project.meongcare.toolbar.viewmodel.ToolbarViewModel
import java.util.Calendar
import java.util.Date

class SymptomFragment : Fragment() {
    lateinit var fragmentSymptomBinding: FragmentSymptomBinding
    lateinit var mainActivity: MainActivity
    lateinit var symptomViewModel: SymptomViewModel
    lateinit var toolbarViewModel: ToolbarViewModel
    private val calendar = Calendar.getInstance()
    lateinit var navController: NavController
    private var currentMonth = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        fragmentSymptomBinding = FragmentSymptomBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity

        calendar.time = Date()
        currentMonth = calendar[Calendar.MONTH]

        symptomViewModel = mainActivity.symptomViewModel
        toolbarViewModel = mainActivity.toolbarViewModel

        navController = findNavController()

        symptomViewModel.run {
            clearLiveData()
            if (toolbarViewModel.selectedDate.value != null) {
                updateSymptomList(1, toolbarViewModel.selectedDate.value!!)
            }
            symptomList.observe(viewLifecycleOwner) {
                fragmentSymptomBinding.run {
                    if (symptomViewModel.symptomList.value.isNullOrEmpty()) {
                        recyclerViewSymptom.visibility = View.GONE
                        textViewSymptomEdit.visibility = View.GONE
                        layoutSymptomNoData.visibility = View.VISIBLE
                    } else {
                        recyclerViewSymptom.visibility = View.VISIBLE
                        textViewSymptomEdit.visibility = View.VISIBLE
                        layoutSymptomNoData.visibility = View.GONE
                    }
                    recyclerViewSymptom.run {
                        adapter = SymptomRecyclerViewAdapter()
                        layoutManager = LinearLayoutManager(context)
                    }
                }
            }
        }

        val dogName = "김대박"

        fragmentSymptomBinding.run {

            textViewSymptomDogName.text = dogName

            textViewSymptomAdd.setOnClickListener {
                navController.navigate(R.id.action_symptom_to_symptomAdd)
            }

            textViewSymptomEdit.setOnClickListener {
                navController.navigate(R.id.action_symptom_to_symptomListEdit)
            }

        }
        return fragmentSymptomBinding.root
    }

    // 이상증상 타임라인
    inner class SymptomRecyclerViewAdapter :
        RecyclerView.Adapter<SymptomRecyclerViewAdapter.SymptomViewHolder>() {
        inner class SymptomViewHolder(itemSymptomBinding: ItemSymptomBinding) :
            RecyclerView.ViewHolder(itemSymptomBinding.root) {
            val itemSymptomName: TextView
            val itemSymptomTime: TextView
            val itemSymptomImg: ImageView

            init {
                itemSymptomName = itemSymptomBinding.textViewItemSymptom
                itemSymptomTime = itemSymptomBinding.textViewItemSymptomTime
                itemSymptomImg = itemSymptomBinding.imageViewItemSymptom

                itemSymptomBinding.root.setOnClickListener {
                    navController.navigate(R.id.action_symptom_to_symptomInfo)
                    symptomViewModel.updateSymptomData(adapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): SymptomViewHolder {
            val itemSymptomBinding = ItemSymptomBinding.inflate(layoutInflater)
            val allViewHolder = SymptomViewHolder(itemSymptomBinding)

            itemSymptomBinding.root.layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )

            return allViewHolder
        }

        override fun getItemCount(): Int {
            return symptomViewModel.symptomList.value!!.size
        }

        override fun onBindViewHolder(
            holder: SymptomViewHolder,
            position: Int,
        ) {
            holder.itemSymptomName.text = symptomViewModel.symptomList.value!![position].note
            holder.itemSymptomTime.text =
                convertDateToTime(symptomViewModel.symptomList.value!![position].dateTime)
            holder.itemSymptomImg.setImageResource(getSymptomImg(symptomViewModel.symptomList.value!![position]))
        }
    }
}
