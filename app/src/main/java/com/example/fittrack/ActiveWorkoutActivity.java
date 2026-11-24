package com.example.fittrack;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActiveWorkoutActivity extends AppCompatActivity implements ActiveExerciseAdapter.OnExerciseStatusChangeListener {

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private long workoutId;
    private String workoutName;

    // Header views
    private ImageButton btnBack;
    private TextView tvActiveWorkoutTitle;
    private TextView tvActiveWorkoutSubtitle;
    private Button btnHeaderFinish;

    // Timer views
    private TextView tvTimerCountdown;
    private TextView tvTimerStatus;
    private Button btnTimer30s;
    private Button btnTimer60s;
    private Button btnTimer90s;
    private Button btnTimerCustom;
    private Button btnTimerStart;
    private Button btnTimerPause;
    private Button btnTimerReset;

    // Exercise list views
    private TextView tvCompletedCounter;
    private RecyclerView rvActiveExercises;
    private Button btnFinishWorkout;

    // Exercise data
    private List<Exercise> exerciseList;
    private ActiveExerciseAdapter exerciseAdapter;

    // Countdown Timer logic
    private CountDownTimer countDownTimer;
    private long totalDurationMillis = 60000; // default: 60s
    private long timeLeftMillis = 60000;
    private boolean isTimerRunning = false;

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

        workoutId = getIntent().getLongExtra(MainActivity.EXTRA_WORKOUT_ID, -1);
        workoutName = getIntent().getStringExtra(MainActivity.EXTRA_WORKOUT_NAME);
        if (workoutName == null) {
            workoutName = "Custom Workout";
        }

        initializeViews();
        setupTimerControls();
        loadWorkoutExercises();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvActiveWorkoutTitle = findViewById(R.id.tvActiveWorkoutTitle);
        tvActiveWorkoutSubtitle = findViewById(R.id.tvActiveWorkoutSubtitle);
        btnHeaderFinish = findViewById(R.id.btnHeaderFinish);

        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        tvTimerStatus = findViewById(R.id.tvTimerStatus);
        btnTimer30s = findViewById(R.id.btnTimer30s);
        btnTimer60s = findViewById(R.id.btnTimer60s);
        btnTimer90s = findViewById(R.id.btnTimer90s);
        btnTimerCustom = findViewById(R.id.btnTimerCustom);
        btnTimerStart = findViewById(R.id.btnTimerStart);
        btnTimerPause = findViewById(R.id.btnTimerPause);
        btnTimerReset = findViewById(R.id.btnTimerReset);

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

        View.OnClickListener finishClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndSaveWorkout();
            }
        };

        btnHeaderFinish.setOnClickListener(finishClickListener);
        btnFinishWorkout.setOnClickListener(finishClickListener);
    }

    private void setupTimerControls() {
        updateTimerDisplay();

        btnTimer30s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(30);
            }
        });

        btnTimer60s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(60);
            }
        });

        btnTimer90s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(90);
            }
        });

        btnTimerCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomTimerDialog();
            }
        });

        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();
            }
        });

        btnTimerPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
            }
        });

        btnTimerReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });
    }

    private void setTimerDuration(int seconds) {
        pauseTimer();
        totalDurationMillis = seconds * 1000L;
        timeLeftMillis = totalDurationMillis;
        tvTimerStatus.setText("Ready (" + seconds + "s)");
        updateTimerDisplay();
    }

    private void showCustomTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Custom Rest Timer");
        builder.setMessage("Enter rest duration in seconds:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("e.g. 45");
        builder.setView(input);

        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String val = input.getText().toString().trim();
                if (!TextUtils.isEmpty(val)) {
                    try {
                        int sec = Integer.parseInt(val);
                        if (sec > 0 && sec <= 600) {
                            setTimerDuration(sec);
                        } else {
                            Toast.makeText(ActiveWorkoutActivity.this, "Enter duration between 1 and 600s", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startTimer() {
        if (isTimerRunning) {
            return;
        }

        if (timeLeftMillis <= 0) {
            timeLeftMillis = totalDurationMillis;
        }

        countDownTimer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timeLeftMillis = 0;
                updateTimerDisplay();
                tvTimerStatus.setText("Time's up! Ready for next set! 🎉");
                Toast.makeText(ActiveWorkoutActivity.this, "Rest complete! Time for the next set!", Toast.LENGTH_LONG).show();
            }
        }.start();

        isTimerRunning = true;
        tvTimerStatus.setText("Resting...");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        tvTimerStatus.setText("Paused");
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        timeLeftMillis = totalDurationMillis;
        updateTimerDisplay();
        tvTimerStatus.setText("Ready");
    }

    private void updateTimerDisplay() {
        int totalSeconds = (int) (timeLeftMillis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        tvTimerCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
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
     * Saves every completed exercise with today's date into workout_history.
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
        long userId = sessionManager.getUserId();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Exercise ex : completedList) {
            int setsDone = ex.getCurrentSets() > 0 ? ex.getCurrentSets() : ex.getSets();
            double weightUsed = ex.getCurrentWeight() >= 0 ? ex.getCurrentWeight() : ex.getWeight();
            databaseHelper.logWorkoutHistory(userId, workoutName, ex.getExerciseName(), setsDone, weightUsed, todayDate);
        }

        Toast.makeText(this, "🎉 Workout saved! " + completedList.size() + " exercises logged to history.", Toast.LENGTH_LONG).show();

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
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
