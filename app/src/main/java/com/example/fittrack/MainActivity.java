package com.example.fittrack;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private TextView tvWelcomeUser;
    private Button btnLogout;
    private LinearLayout llWorkoutsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Verify user is logged in; if not, redirect to LoginActivity
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnLogout = findViewById(R.id.btnLogout);
        llWorkoutsContainer = findViewById(R.id.llWorkoutsContainer);

        // Display logged-in user name
        String username = sessionManager.getUsername();
        tvWelcomeUser.setText("Welcome, " + username + "!");

        // Handle logout
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionManager.logout();
                redirectToLogin();
            }
        });

        // Load pre-populated workouts and their exercises from SQLite
        loadUserWorkouts();
    }

    private void loadUserWorkouts() {
        long userId = sessionManager.getUserId();
        Cursor workoutCursor = databaseHelper.getUserWorkouts(userId);

        llWorkoutsContainer.removeAllViews();

        if (workoutCursor != null) {
            try {
                while (workoutCursor.moveToNext()) {
                    long workoutId = workoutCursor.getLong(workoutCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WORKOUT_ID));
                    String workoutName = workoutCursor.getString(workoutCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WORKOUT_NAME));

                    // Card container for each workout
                    LinearLayout workoutCard = new LinearLayout(this);
                    workoutCard.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.match_parent,
                            LinearLayout.LayoutParams.wrap_content
                    );
                    cardParams.setMargins(0, 16, 0, 0);
                    workoutCard.setLayoutParams(cardParams);
                    workoutCard.setBackgroundResource(R.drawable.bg_card);
                    workoutCard.setPadding(20, 16, 20, 16);

                    // Workout title
                    TextView tvWorkoutName = new TextView(this);
                    tvWorkoutName.setText("🏋️ " + workoutName);
                    tvWorkoutName.setTextSize(16);
                    tvWorkoutName.setTypeface(null, Typeface.BOLD);
                    tvWorkoutName.setTextColor(getResources().getColor(R.color.text_primary));
                    workoutCard.addView(tvWorkoutName);

                    // Fetch exercises for this workout
                    Cursor exerciseCursor = databaseHelper.getWorkoutExercises(workoutId);
                    if (exerciseCursor != null) {
                        try {
                            while (exerciseCursor.moveToNext()) {
                                String exerciseName = exerciseCursor.getString(exerciseCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXERCISE_NAME));
                                int sets = exerciseCursor.getInt(exerciseCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXERCISE_SETS));
                                int reps = exerciseCursor.getInt(exerciseCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXERCISE_REPS));
                                double weight = exerciseCursor.getDouble(exerciseCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXERCISE_WEIGHT));

                                TextView tvExercise = new TextView(this);
                                String exerciseDetail = "  • " + exerciseName + " (" + sets + " sets × " + reps + " reps" + (weight > 0 ? " @ " + weight + " kg" : "") + ")";
                                tvExercise.setText(exerciseDetail);
                                tvExercise.setTextSize(14);
                                tvExercise.setTextColor(getResources().getColor(R.color.text_secondary));
                                tvExercise.setPadding(0, 6, 0, 0);
                                workoutCard.addView(tvExercise);
                            }
                        } finally {
                            exerciseCursor.close();
                        }
                    }

                    llWorkoutsContainer.addView(workoutCard);
                }
            } finally {
                workoutCursor.close();
            }
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
