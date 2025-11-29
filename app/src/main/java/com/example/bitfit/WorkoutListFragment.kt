package com.example.bitfit.ui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bitfit.BitFitApplication
import com.example.bitfit.R
import com.example.bitfit.WorkoutActivity
import com.example.bitfit.WorkoutAdapter
import com.example.bitfit.WorkoutDao
import com.example.bitfit.WorkoutEntity
import com.example.bitfit.WorkoutUi
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutListFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var empty: TextView
    private lateinit var fab: FloatingActionButton

    private lateinit var workoutDao: WorkoutDao
    private var currentEntities: List<WorkoutEntity> = emptyList()

    private val adapter = WorkoutAdapter(onClick = { workout ->
        val i = Intent(requireContext(), WorkoutActivity::class.java).apply {
            putExtra("workout_name", workout.name)
            putExtra("workout_id", workout.id)
        }
        startActivity(i)
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_workout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workoutDao =
            (requireActivity().application as BitFitApplication).database.workoutDao()

        rv = view.findViewById(R.id.rvWorkouts)
        empty = view.findViewById(R.id.tvEmptyState)
        fab = view.findViewById(R.id.fabAddWorkout)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        render(emptyList())

        fab.setOnClickListener {
            val i = Intent(requireContext(), WorkoutActivity::class.java).apply {
                putExtra("workout_id", -1L)
            }
            startActivity(i)
        }

        attachSwipeToDelete()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutDao.getAll().collect { entities ->
                    currentEntities = entities
                    val uiItems = entities.map { entity ->
                        WorkoutUi(
                            id = entity.id,
                            name = entity.name,
                            dateLabel = entity.date,
                            exerciseCount = entity.exerciseCount
                        )
                    }
                    render(uiItems)
                }
            }
        }
    }

    private fun render(items: List<WorkoutUi>) {
        adapter.submit(items)
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun attachSwipeToDelete() {
        val deleteIcon: Drawable? =
            ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
        val red = Paint().apply { color = 0xFFE53935.toInt() }

        val touchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos in currentEntities.indices) {
                    val entity = currentEntities[pos]
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        workoutDao.delete(entity)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (dX < 0) {
                    val background = RectF(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(background, red)

                    deleteIcon?.let { icon ->
                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(touchHelper).attachToRecyclerView(rv)
    }
}