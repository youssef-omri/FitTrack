package com.example.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WorkoutAdapter.OnWorkoutClickListener {

    public static final String EXTRA_WORKOUT_ID = "extra_workout_id";
    public static final String EXTRA_WORKOUT_NAME = "extra_workout_name";

    // Built-in exercises for autocomplete selection
    private static final String[] BUILT_IN_EXERCISES = new String[]{
            "Bench Press",
            "Squat",
            "Deadlift",
            "Pull-ups",
            "Barbell Row",
            "Overhead Press",
            "Incline Dumbbell Press",
            "Lat Pulldown",
            "Dumbbell Bicep Curl",
            "Tricep Pushdown",
            "Leg Press",
            "Lunges",
            "Push-ups",
            "Dips",
            "Plank",
            "Cable Crossover",
            "Leg Curl",
            "Calf Raise"
    };

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private TextView tvWelcomeUser;
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
        btnLogout = findViewById(R.id.btnLogout);
        btnHistory = findViewById(R.id.btnHistory);
        btnCreateWorkout = findViewById(R.id.btnCreateWorkout);
        rvWorkouts = findViewById(R.id.rvWorkouts);
        llEmptyState = findViewById(R.id.llEmptyState);

        // Display logged-in user name
        String username = sessionManager.getUsername();
        tvWelcomeUser.setText("Welcome, " + username + "!");

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

        // Open Create Custom Workout dialog
        btnCreateWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateWorkoutDialog();
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
        }
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

    /**
     * Shows dialog to create a new custom workout routine with custom/built-in exercises.
     */
    private void showCreateWorkoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_workout, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final EditText etRoutineName = dialogView.findViewById(R.id.etRoutineName);
        final ImageView ivSelectedExerciseThumb = dialogView.findViewById(R.id.ivSelectedExerciseThumb);
        final AutoCompleteTextView actvExerciseName = dialogView.findViewById(R.id.actvExerciseName);
        final EditText etExerciseSets = dialogView.findViewById(R.id.etExerciseSets);
        final EditText etExerciseReps = dialogView.findViewById(R.id.etExerciseReps);
        final EditText etExerciseWeight = dialogView.findViewById(R.id.etExerciseWeight);
        final Button btnAddExerciseItem = dialogView.findViewById(R.id.btnAddExerciseItem);
        final LinearLayout llAddedExercisesList = dialogView.findViewById(R.id.llAddedExercisesList);
        final TextView tvNoExercisesYet = dialogView.findViewById(R.id.tvNoExercisesYet);
        final Button btnCancelRoutine = dialogView.findViewById(R.id.btnCancelRoutine);
        final Button btnSaveRoutine = dialogView.findViewById(R.id.btnSaveRoutine);

        // Pre-fill AutoComplete with built-in exercises and demonstration thumbnails
        ExerciseDropdownAdapter exerciseAdapter = new ExerciseDropdownAdapter(
                this,
                BUILT_IN_EXERCISES
        );
        actvExerciseName.setAdapter(exerciseAdapter);
        ExerciseImageHelper.getInstance().loadExerciseImage(this, "", ivSelectedExerciseThumb);

        actvExerciseName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                String selected = item != null ? item.toString() : "";
                ExerciseImageHelper.getInstance().loadExerciseImage(MainActivity.this, selected, ivSelectedExerciseThumb);
            }
        });

        actvExerciseName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String typed = s != null ? s.toString().trim() : "";
                ExerciseImageHelper.getInstance().loadExerciseImage(MainActivity.this, typed, ivSelectedExerciseThumb);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        final List<Exercise> pendingExercises = new ArrayList<>();

        // Add exercise item to pending list
        btnAddExerciseItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String exName = actvExerciseName.getText().toString().trim();
                String setsStr = etExerciseSets.getText().toString().trim();
                String repsStr = etExerciseReps.getText().toString().trim();
                String weightStr = etExerciseWeight.getText().toString().trim();

                if (TextUtils.isEmpty(exName)) {
                    actvExerciseName.setError("Enter or select an exercise");
                    actvExerciseName.requestFocus();
                    return;
                }

                int sets = 3;
                if (!TextUtils.isEmpty(setsStr)) {
                    try {
                        sets = Integer.parseInt(setsStr);
                    } catch (NumberFormatException ignored) {
                    }
                }

                int reps = 10;
                if (!TextUtils.isEmpty(repsStr)) {
                    try {
                        reps = Integer.parseInt(repsStr);
                    } catch (NumberFormatException ignored) {
                    }
                }

                double weight = 0.0;
                if (!TextUtils.isEmpty(weightStr)) {
                    try {
                        weight = Double.parseDouble(weightStr);
                    } catch (NumberFormatException ignored) {
                    }
                }

                final Exercise newExercise = new Exercise(exName, sets, reps, weight);
                pendingExercises.add(newExercise);

                // Add to view list
                addExerciseChipToDialog(llAddedExercisesList, tvNoExercisesYet, pendingExercises, newExercise);

                // Clear input fields for next exercise
                actvExerciseName.setText("");
                etExerciseSets.setText("3");
                etExerciseReps.setText("10");
                etExerciseWeight.setText("0");
                actvExerciseName.requestFocus();
            }
        });

        // Cancel
        btnCancelRoutine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Save Routine
        btnSaveRoutine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String routineName = etRoutineName.getText().toString().trim();
                if (TextUtils.isEmpty(routineName)) {
                    etRoutineName.setError("Routine name is required");
                    etRoutineName.requestFocus();
                    return;
                }

                // If user typed an exercise into the inputs without pressing "+ Add", include it
                String pendingExName = actvExerciseName.getText().toString().trim();
                if (!TextUtils.isEmpty(pendingExName)) {
                    int sets = 3;
                    try {
                        sets = Integer.parseInt(etExerciseSets.getText().toString().trim());
                    } catch (Exception ignored) {
                    }
                    int reps = 10;
                    try {
                        reps = Integer.parseInt(etExerciseReps.getText().toString().trim());
                    } catch (Exception ignored) {
                    }
                    double weight = 0.0;
                    try {
                        weight = Double.parseDouble(etExerciseWeight.getText().toString().trim());
                    } catch (Exception ignored) {
                    }
                    pendingExercises.add(new Exercise(pendingExName, sets, reps, weight));
                }

                if (pendingExercises.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please add at least one exercise to this routine.", Toast.LENGTH_SHORT).show();
                    return;
                }

                long userId = sessionManager.getUserId();
                long workoutId = databaseHelper.createCustomWorkout(userId, routineName, pendingExercises);

                if (workoutId != -1) {
                    Toast.makeText(MainActivity.this, "Routine '" + routineName + "' created!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadUserWorkouts();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to create routine. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void addExerciseChipToDialog(final LinearLayout container, final TextView emptyView, final List<Exercise> list, final Exercise exercise) {
        emptyView.setVisibility(View.GONE);

        final View chipView = LayoutInflater.from(this).inflate(R.layout.item_added_exercise_chip, container, false);
        ImageView ivThumb = chipView.findViewById(R.id.ivAddedExerciseThumb);
        TextView tvName = chipView.findViewById(R.id.tvAddedExerciseName);
        TextView tvDetails = chipView.findViewById(R.id.tvAddedExerciseDetails);
        ImageButton btnRemove = chipView.findViewById(R.id.btnRemoveAddedExercise);

        ExerciseImageHelper.getInstance().loadExerciseImage(this, exercise.getExerciseName(), ivThumb);
        tvName.setText(exercise.getExerciseName());
        String weightText = exercise.getWeight() > 0 ? " @ " + exercise.getWeight() + " kg" : "";
        tvDetails.setText(exercise.getSets() + " sets × " + exercise.getReps() + " reps" + weightText);

        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.remove(exercise);
                container.removeView(chipView);
                if (list.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                }
            }
        });

        container.addView(chipView);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
