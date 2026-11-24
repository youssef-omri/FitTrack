package com.example.fittrack;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActiveWorkoutActivity extends AppCompatActivity implements ActiveExerciseAdapter.OnExerciseStatusChangeListener {

    private static final String STATE_ACCUMULATED_MS = "state_accumulated_ms";
    private static final String STATE_START_ELAPSED_MS = "state_start_elapsed_ms";
    private static final String STATE_IS_RUNNING = "state_is_running";

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private long workoutId;
    private String workoutName;

    // Header views
    private ImageButton btnBack;
    private TextView tvActiveWorkoutTitle;
    private TextView tvActiveWorkoutSubtitle;

    // Stopwatch views
    private TextView tvStopwatchTime;
    private TextView tvStopwatchStatus;
    private Button btnStopwatchToggle;

    // Exercise list views
    private TextView tvCompletedCounter;
    private RecyclerView rvActiveExercises;
    private Button btnFinishWorkout;

    // Exercise data
    private List<Exercise> exerciseList;
    private ActiveExerciseAdapter exerciseAdapter;

    // Upward-counting Workout Duration Stopwatch
    private final Handler stopwatchHandler = new Handler(Looper.getMainLooper());
    private long startTimeElapsedMs = 0L;
    private long accumulatedTimeMs = 0L;
    private boolean isStopwatchRunning = false;

    private final Runnable stopwatchRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStopwatchRunning) {
                updateStopwatchDisplay();
                stopwatchHandler.postDelayed(this, 1000L);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_active_workout);

        databaseHelper = new DatabaseHelper(this);
        ExerciseImageHelper.getInstance().prefetchExercises(this);

        workoutId = getIntent().getLongExtra(MainActivity.EXTRA_WORKOUT_ID, -1);
        workoutName = getIntent().getStringExtra(MainActivity.EXTRA_WORKOUT_NAME);
        if (workoutName == null) {
            workoutName = "Custom Workout";
        }

        initializeViews();
        setupStopwatch(savedInstanceState);
        loadWorkoutExercises();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvActiveWorkoutTitle = findViewById(R.id.tvActiveWorkoutTitle);
        tvActiveWorkoutSubtitle = findViewById(R.id.tvActiveWorkoutSubtitle);

        tvStopwatchTime = findViewById(R.id.tvStopwatchTime);
        tvStopwatchStatus = findViewById(R.id.tvStopwatchStatus);
        btnStopwatchToggle = findViewById(R.id.btnStopwatchToggle);

        tvCompletedCounter = findViewById(R.id.tvCompletedCounter);
        rvActiveExercises = findViewById(R.id.rvActiveExercises);
        btnFinishWorkout = findViewById(R.id.btnFinishWorkout);

        tvActiveWorkoutTitle.setText(workoutName);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmExit();
            }
        });

        btnStopwatchToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStopwatchRunning) {
                    pauseStopwatch();
                } else {
                    startStopwatch();
                }
            }
        });

        btnFinishWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndSaveWorkout();
            }
        });
    }

    private void setupStopwatch(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            accumulatedTimeMs = savedInstanceState.getLong(STATE_ACCUMULATED_MS, 0L);
            startTimeElapsedMs = savedInstanceState.getLong(STATE_START_ELAPSED_MS, SystemClock.elapsedRealtime());
            isStopwatchRunning = savedInstanceState.getBoolean(STATE_IS_RUNNING, true);
            if (isStopwatchRunning) {
                stopwatchHandler.removeCallbacks(stopwatchRunnable);
                stopwatchHandler.post(stopwatchRunnable);
            }
            updateStopwatchDisplay();
            updateStopwatchControlsUI();
        } else {
            // Start upward-counting stopwatch automatically upon entering ActiveWorkoutActivity
            startStopwatch();
        }
    }

    private void startStopwatch() {
        if (isStopwatchRunning) {
            return;
        }
        startTimeElapsedMs = SystemClock.elapsedRealtime();
        isStopwatchRunning = true;
        stopwatchHandler.removeCallbacks(stopwatchRunnable);
        stopwatchHandler.post(stopwatchRunnable);
        updateStopwatchControlsUI();
    }

    private void pauseStopwatch() {
        if (!isStopwatchRunning) {
            return;
        }
        accumulatedTimeMs += Math.max(0L, SystemClock.elapsedRealtime() - startTimeElapsedMs);
        isStopwatchRunning = false;
        stopwatchHandler.removeCallbacks(stopwatchRunnable);
        updateStopwatchDisplay();
        updateStopwatchControlsUI();
    }

    private long getElapsedMillis() {
        if (isStopwatchRunning) {
            return accumulatedTimeMs + Math.max(0L, SystemClock.elapsedRealtime() - startTimeElapsedMs);
        }
        return accumulatedTimeMs;
    }

    private void updateStopwatchDisplay() {
        long totalSeconds = getElapsedMillis() / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        tvStopwatchTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateStopwatchControlsUI() {
        if (isStopwatchRunning) {
            btnStopwatchToggle.setText(R.string.pause_stopwatch);
            btnStopwatchToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
            tvStopwatchStatus.setText(R.string.stopwatch_running);
            tvStopwatchStatus.setBackgroundResource(R.drawable.bg_badge_success);
            tvStopwatchStatus.setTextColor(ContextCompat.getColor(this, R.color.accent));
            tvActiveWorkoutSubtitle.setText("In Progress");
        } else {
            btnStopwatchToggle.setText(R.string.resume_stopwatch);
            btnStopwatchToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
            tvStopwatchStatus.setText(R.string.stopwatch_paused);
            tvStopwatchStatus.setBackgroundResource(R.drawable.bg_badge_neutral);
            tvStopwatchStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvActiveWorkoutSubtitle.setText("Paused");
        }
    }

    private String formatDurationForHistory(long elapsedMillis) {
        long totalSeconds = elapsedMillis / 1000L;
        long totalMinutes = totalSeconds / 60L;
        if (totalMinutes >= 60L) {
            long hours = totalMinutes / 60L;
            long remainingMinutes = totalMinutes % 60L;
            return remainingMinutes > 0
                    ? hours + "h " + remainingMinutes + " min"
                    : hours + "h";
        } else if (totalMinutes >= 1L) {
            return totalMinutes + " min";
        } else {
            return Math.max(1L, totalSeconds) + " sec";
        }
    }

    private void loadWorkoutExercises() {
        exerciseList = new ArrayList<>();
        if (workoutId != -1) {
            exerciseList = databaseHelper.getExercisesForWorkout(workoutId);
        }

        exerciseAdapter = new ActiveExerciseAdapter(this, exerciseList, this);
        rvActiveExercises.setLayoutManager(new LinearLayoutManager(this));
        rvActiveExercises.setAdapter(exerciseAdapter);

        updateCompletedCounter();
    }

    @Override
    public void onExerciseCompletedChanged(Exercise exercise, boolean isCompleted) {
        updateCompletedCounter();
    }

    private void updateCompletedCounter() {
        int completed = 0;
        if (exerciseList != null) {
            for (Exercise ex : exerciseList) {
                if (ex.isCompleted()) {
                    completed++;
                }
            }
            int total = exerciseList.size();
            tvCompletedCounter.setText(completed + " / " + total + " completed");
        }
    }

    /**
     * Saves every completed exercise with today's date and total workout duration into workout_history.
     */
    private void finishAndSaveWorkout() {
        if (exerciseList == null || exerciseList.isEmpty()) {
            Toast.makeText(this, "No exercises in this workout.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Count completed
        final List<Exercise> completedList = new ArrayList<>();
        for (Exercise ex : exerciseList) {
            if (ex.isCompleted()) {
                completedList.add(ex);
            }
        }

        if (completedList.isEmpty()) {
            // Prompt user if they want to mark all done or save nothing
            new AlertDialog.Builder(this)
                    .setTitle("No Exercises Marked Done")
                    .setMessage("None of the exercises are marked as 'Done'. Would you like to mark all as completed and save, or continue working out?")
                    .setPositiveButton("Mark All Done & Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (Exercise ex : exerciseList) {
                                ex.setCompleted(true);
                                completedList.add(ex);
                            }
                            saveCompletedExercisesToHistory(completedList);
                        }
                    })
                    .setNegativeButton("Keep Working", null)
                    .show();
        } else {
            saveCompletedExercisesToHistory(completedList);
        }
    }

    private void saveCompletedExercisesToHistory(List<Exercise> completedList) {
        long elapsedMillis = getElapsedMillis();
        pauseStopwatch();
        String formattedDuration = formatDurationForHistory(elapsedMillis);

        long userId = sessionManager.getUserId();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Exercise ex : completedList) {
            int setsDone = ex.getCurrentSets() > 0 ? ex.getCurrentSets() : ex.getSets();
            double weightUsed = ex.getCurrentWeight() >= 0 ? ex.getCurrentWeight() : ex.getWeight();
            databaseHelper.logWorkoutHistory(
                    userId,
                    workoutName,
                    ex.getExerciseName(),
                    setsDone,
                    weightUsed,
                    todayDate,
                    formattedDuration
            );
        }

        Toast.makeText(
                this,
                "🎉 Workout saved (" + formattedDuration + ")! " + completedList.size() + " exercises logged to history.",
                Toast.LENGTH_LONG
        ).show();

        // Navigate to HistoryActivity so the user can immediately view their progress
        Intent intent = new Intent(ActiveWorkoutActivity.this, HistoryActivity.class);
        startActivity(intent);
        finish();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Workout?")
                .setMessage("Are you sure you want to leave? Unsaved workout progress will be lost.")
                .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_ACCUMULATED_MS, accumulatedTimeMs);
        outState.putLong(STATE_START_ELAPSED_MS, startTimeElapsedMs);
        outState.putBoolean(STATE_IS_RUNNING, isStopwatchRunning);
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopwatchHandler.removeCallbacks(stopwatchRunnable);
    }
}
