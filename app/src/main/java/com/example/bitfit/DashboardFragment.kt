package com.example.bitfit

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao

    private lateinit var totalWorkoutsText: TextView
    private lateinit var totalExercisesText: TextView
    private lateinit var lastWorkoutText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get DAOs
        val db = (requireActivity().application as BitFitApplication).database
        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()

        // Views
        totalWorkoutsText = view.findViewById(R.id.tvTotalWorkouts)
        totalExercisesText = view.findViewById(R.id.tvTotalExercises)
        lastWorkoutText = view.findViewById(R.id.tvLastWorkout)

        // Collect workouts from DB
        viewLifecycleOwner.lifecycleScope.launch {
            workoutDao.getAll().collectLatest { workouts ->
                val totalWorkouts = workouts.size
                val totalExercises = workouts.sumOf { it.exerciseCount }

                totalWorkoutsText.text = totalWorkouts.toString()
                totalExercisesText.text = totalExercises.toString()

                val last = workouts.maxByOrNull { it.id }  // or by date if you prefer
                lastWorkoutText.text = if (last != null) {
                    "${last.name} • ${last.date} • ${last.exerciseCount} exercises"
                } else {
                    "No workouts yet"
                }
            }
        }
    }
}