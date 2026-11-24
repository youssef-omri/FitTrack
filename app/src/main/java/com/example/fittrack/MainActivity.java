package com.example.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WorkoutAdapter.OnWorkoutClickListener {

    public static final String EXTRA_WORKOUT_ID = "extra_workout_id";
    public static final String EXTRA_WORKOUT_NAME = "extra_workout_name";

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private TextView tvWelcomeUser;
    private ImageButton btnThemeToggle;
    private Button btnLogout;
    private Button btnHistory;
    private Button btnCreateWorkout;
    private RecyclerView rvWorkouts;
    private LinearLayout llEmptyState;
    private WorkoutAdapter workoutAdapter;
    private List<Workout> workoutList;

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
        ExerciseImageHelper.getInstance().prefetchExercises(this);

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnLogout = findViewById(R.id.btnLogout);
        btnHistory = findViewById(R.id.btnHistory);
        btnCreateWorkout = findViewById(R.id.btnCreateWorkout);
        rvWorkouts = findViewById(R.id.rvWorkouts);
        llEmptyState = findViewById(R.id.llEmptyState);

        // Display logged-in user name
        String username = sessionManager.getUsername();
        tvWelcomeUser.setText("Welcome, " + username + "!");

        // Configure Dark/Light mode toggle icon and click listener
        updateThemeToggleIcon();
        btnThemeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean nextDark = !ThemeManager.isDarkMode(MainActivity.this);
                ThemeManager.setDarkMode(MainActivity.this, nextDark);
            }
        });

        // Setup RecyclerView
        workoutList = new ArrayList<>();
        workoutAdapter = new WorkoutAdapter(this, workoutList, this);
        rvWorkouts.setLayoutManager(new LinearLayoutManager(this));
        rvWorkouts.setAdapter(workoutAdapter);

        // Handle logout
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionManager.logout();
                redirectToLogin();
            }
        });

        // Navigate to HistoryActivity
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        // Open visual Muscle-Group & Exercise routine creator
        btnCreateWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateEditRoutineActivity.class);
                startActivity(intent);
            }
        });

        // Load workouts from SQLite
        loadUserWorkouts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadUserWorkouts();
            updateThemeToggleIcon();
        }
    }

    private void updateThemeToggleIcon() {
        if (btnThemeToggle == null) {
            return;
        }
        boolean isDark = ThemeManager.isDarkMode(this);
        btnThemeToggle.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
    }

    private void loadUserWorkouts() {
        long userId = sessionManager.getUserId();
        List<Workout> userWorkouts = databaseHelper.getUserWorkoutList(userId);

        workoutList.clear();
        if (userWorkouts != null && !userWorkouts.isEmpty()) {
            workoutList.addAll(userWorkouts);
            llEmptyState.setVisibility(View.GONE);
            rvWorkouts.setVisibility(View.VISIBLE);
        } else {
            llEmptyState.setVisibility(View.VISIBLE);
            rvWorkouts.setVisibility(View.GONE);
        }
        workoutAdapter.notifyDataSetChanged();
    }

    @Override
    public void onWorkoutClick(Workout workout) {
        // Start active workout session
        Intent intent = new Intent(MainActivity.this, ActiveWorkoutActivity.class);
        intent.putExtra(EXTRA_WORKOUT_ID, workout.getId());
        intent.putExtra(EXTRA_WORKOUT_NAME, workout.getName());
        startActivity(intent);
    }

    @Override
    public void onEditWorkoutClick(Workout workout) {
        Intent intent = new Intent(MainActivity.this, CreateEditRoutineActivity.class);
        intent.putExtra(CreateEditRoutineActivity.EXTRA_WORKOUT_ID, workout.getId());
        startActivity(intent);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
