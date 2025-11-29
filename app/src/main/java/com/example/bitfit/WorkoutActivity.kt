package com.example.bitfit

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class WorkoutActivity : AppCompatActivity() {

    private lateinit var titleEdit: EditText
    private lateinit var pencilIcon: ImageView
    private lateinit var addExerciseBtn: MaterialButton
    private lateinit var endWorkoutBtn: MaterialButton
    private lateinit var rvExercises: RecyclerView

    private val adapter = ExerciseAdapter()

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao

    // -1L means "new workout"
    private var workoutId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        // Get DAOs
        val db = (application as BitFitApplication).database
        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()

        // Views
        titleEdit = findViewById(R.id.etWorkoutTitle)
        // pencilIcon = findViewById(R.id.ivPencil)
        addExerciseBtn = findViewById(R.id.btnAddExercise)
        endWorkoutBtn = findViewById(R.id.btnEndWorkout)
        rvExercises = findViewById(R.id.rvExercises)

        rvExercises.layoutManager = LinearLayoutManager(this)
        rvExercises.adapter = adapter

        // Read intent extras
        workoutId = intent.getLongExtra("workout_id", -1L)
        val existingName = intent.getStringExtra("workout_name")

        if (!existingName.isNullOrBlank()) {
            titleEdit.setText(existingName)
        }

        // Pencil focuses and moves cursor to end
        // pencilIcon.setOnClickListener {
        //     titleEdit.requestFocus()
        //     titleEdit.setSelection(titleEdit.text.length)
        // }

        // Add a blank exercise row
        addExerciseBtn.setOnClickListener {
            adapter.add(ExerciseUi(name = "", sets = "", reps = ""))
            rvExercises.smoothScrollToPosition(adapter.itemCount - 1)
        }

        // If editing an existing workout, load its exercises from DB
        if (workoutId != -1L) {
            lifecycleScope.launch {
                exerciseDao.getForWorkout(workoutId).collectLatest { entities ->
                    val uiItems = entities.map {
                        ExerciseUi(
                            name = it.name,
                            sets = it.sets,
                            reps = it.reps
                        )
                    }
                    adapter.setItems(uiItems)
                }
            }
        }

        // When user taps "End workout"
        endWorkoutBtn.setOnClickListener {
            val workoutName = titleEdit.text.toString().ifBlank { "Workout" }
            val exerciseItems = adapter.getItems()
            val exerciseCount = exerciseItems.size
            val dateLabel = todayLabel()

            lifecycleScope.launch(Dispatchers.IO) {
                if (workoutId == -1L) {
                    // NEW workout → insert workout, then exercises
                    val workoutEntity = WorkoutEntity(
                        name = workoutName,
                        date = dateLabel,
                        exerciseCount = exerciseCount
                    )
                    val newId = workoutDao.insert(workoutEntity)

                    val exerciseEntities = exerciseItems.map { ui ->
                        ExerciseEntity(
                            workoutId = newId,
                            name = ui.name,
                            sets = ui.sets,
                            reps = ui.reps
                        )
                    }
                    if (exerciseEntities.isNotEmpty()) {
                        exerciseDao.insertAll(exerciseEntities)
                    }

                } else {
                    // EXISTING workout → update + replace exercises
                    val workoutEntity = WorkoutEntity(
                        id = workoutId,
                        name = workoutName,
                        date = dateLabel,
                        exerciseCount = exerciseCount
                    )
                    workoutDao.update(workoutEntity)

                    // Replace exercises for this workout
                    exerciseDao.deleteByWorkoutId(workoutId)
                    val exerciseEntities = exerciseItems.map { ui ->
                        ExerciseEntity(
                            workoutId = workoutId,
                            name = ui.name,
                            sets = ui.sets,
                            reps = ui.reps
                        )
                    }
                    if (exerciseEntities.isNotEmpty()) {
                        exerciseDao.insertAll(exerciseEntities)
                    }
                }

                // Go back to main screen on main thread
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun todayLabel(): String {
        val now = Calendar.getInstance()
        return android.text.format.DateFormat.format("MMM d, yyyy", now).toString()
    }
}