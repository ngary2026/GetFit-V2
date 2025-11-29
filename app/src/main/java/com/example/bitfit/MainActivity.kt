package com.example.bitfit

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var rv: RecyclerView
    private lateinit var empty: TextView
    private lateinit var fab: FloatingActionButton

    private lateinit var workoutDao: WorkoutDao
    private var currentEntities: List<WorkoutEntity> = emptyList()

    private val adapter = WorkoutAdapter(onClick = { workout ->
        // Navigate to workout page (detail)
        val i = Intent(this, WorkoutActivity::class.java).apply {
            putExtra("workout_name", workout.name)
            putExtra("workout_id", workout.id)
        }
        startActivity(i)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        workoutDao = (application as BitFitApplication).database.workoutDao()

        supportActionBar?.title = getString(R.string.app_name)

        rv = findViewById(R.id.rvWorkouts)
        empty = findViewById(R.id.tvEmptyState)
        fab = findViewById(R.id.fabAddWorkout)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        render(emptyList())

        fab.setOnClickListener {
            // New workout (id = -1L means a new workout)
            val i = Intent(this, WorkoutActivity::class.java).apply {
                putExtra("workout_id", -1L)
            }
            startActivity(i)
        }

        attachSwipeToDelete()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    private fun todayLabel(): String {
        // simple label without java.time for brevity; replace with DateTimeFormatter later
        return "Today"
    }

    private fun render(items: List<WorkoutUi>) {
        adapter.submit(items)
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun attachSwipeToDelete() {
        val deleteIcon: Drawable? = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete)
        val red = Paint().apply { color = 0xFFE53935.toInt() } // Material Red 600
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
                    lifecycleScope.launch(Dispatchers.IO) {
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
                    // Red background
                    val background = RectF(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(background, red)

                    // Trash icon
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