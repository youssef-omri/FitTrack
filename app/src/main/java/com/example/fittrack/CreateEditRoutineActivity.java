package com.example.fittrack;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Create/Edit Workout Routine screen with a 3-step visual Muscle-Group & Exercise Picker:
 * Step A: 2-column grid of all 11 major muscle groups with anatomical vector illustrations.
 * Step B: List of exercises for the chosen muscle with Wger demonstration thumbnails + custom option.
 * Step C: Bottom sheet to configure Sets, Reps, and Weight (kg) before adding to the routine.
 */
public class CreateEditRoutineActivity extends AppCompatActivity
        implements MuscleGroupAdapter.OnMuscleGroupClickListener, MuscleExerciseAdapter.OnExerciseSelectedListener {

    public static final String EXTRA_WORKOUT_ID = "extra_workout_id";

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private ImageButton btnRoutineBack;
    private TextView tvRoutineScreenTitle;
    private NestedScrollView nsvRoutineContent;
    private EditText etRoutineName;
    private TextView tvRoutineExerciseCount;
    private LinearLayout llRoutineExercisesList;
    private TextView tvRoutineNoExercises;

    // Step A views
    private LinearLayout llStepAMuscleGrid;
    private RecyclerView rvMuscleGroups;
    private MuscleGroupAdapter muscleGroupAdapter;

    // Step B views
    private LinearLayout llStepBExercises;
    private ImageView ivSelectedMuscleIcon;
    private TextView tvSelectedMuscleTitle;
    private Button btnBackToMuscles;
    private RecyclerView rvMuscleExercises;
    private Button btnAddCustomExercise;
    private MuscleExerciseAdapter muscleExerciseAdapter;

    private Button btnSaveRoutine;

    private long editingWorkoutId = -1L;
    private List<MuscleGroup> muscleGroups;
    private MuscleGroup selectedMuscleGroup;
    private final List<Exercise> pendingExercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_create_edit_routine);

        databaseHelper = new DatabaseHelper(this);
        ExerciseImageHelper.getInstance().prefetchExercises(this);

        editingWorkoutId = getIntent().getLongExtra(EXTRA_WORKOUT_ID, -1L);

        initializeViews();
        setupPickers();
        loadExistingRoutineIfEditing();
        refreshAddedExercisesList();
    }

    private void initializeViews() {
        btnRoutineBack = findViewById(R.id.btnRoutineBack);
        tvRoutineScreenTitle = findViewById(R.id.tvRoutineScreenTitle);
        nsvRoutineContent = findViewById(R.id.nsvRoutineContent);
        etRoutineName = findViewById(R.id.etRoutineName);
        tvRoutineExerciseCount = findViewById(R.id.tvRoutineExerciseCount);
        llRoutineExercisesList = findViewById(R.id.llRoutineExercisesList);
        tvRoutineNoExercises = findViewById(R.id.tvRoutineNoExercises);

        llStepAMuscleGrid = findViewById(R.id.llStepAMuscleGrid);
        rvMuscleGroups = findViewById(R.id.rvMuscleGroups);

        llStepBExercises = findViewById(R.id.llStepBExercises);
        ivSelectedMuscleIcon = findViewById(R.id.ivSelectedMuscleIcon);
        tvSelectedMuscleTitle = findViewById(R.id.tvSelectedMuscleTitle);
        btnBackToMuscles = findViewById(R.id.btnBackToMuscles);
        rvMuscleExercises = findViewById(R.id.rvMuscleExercises);
        btnAddCustomExercise = findViewById(R.id.btnAddCustomExercise);

        btnSaveRoutine = findViewById(R.id.btnSaveRoutine);

        btnRoutineBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackNavigation();
            }
        });

        btnBackToMuscles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMuscleGrid();
            }
        });

        btnAddCustomExercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCustomExerciseDialog();
            }
        });

        btnSaveRoutine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRoutine();
            }
        });
    }

    private void setupPickers() {
        muscleGroups = MuscleGroup.getAllMuscleGroups(this);
        muscleGroupAdapter = new MuscleGroupAdapter(this, muscleGroups, this);
        rvMuscleGroups.setLayoutManager(new GridLayoutManager(this, 2));
        rvMuscleGroups.setNestedScrollingEnabled(false);
        rvMuscleGroups.setAdapter(muscleGroupAdapter);

        muscleExerciseAdapter = new MuscleExerciseAdapter(this, this);
        rvMuscleExercises.setLayoutManager(new LinearLayoutManager(this));
        rvMuscleExercises.setNestedScrollingEnabled(false);
        rvMuscleExercises.setAdapter(muscleExerciseAdapter);
    }

    private void loadExistingRoutineIfEditing() {
        if (editingWorkoutId == -1L) {
            tvRoutineScreenTitle.setText(R.string.create_routine_title);
            btnSaveRoutine.setText("Save Routine");
            return;
        }

        Workout existing = databaseHelper.getWorkoutById(editingWorkoutId);
        if (existing != null) {
            tvRoutineScreenTitle.setText(R.string.edit_routine_title);
            btnSaveRoutine.setText("Update Routine");
            etRoutineName.setText(existing.getName());
            pendingExercises.clear();
            if (existing.getExercises() != null) {
                pendingExercises.addAll(existing.getExercises());
            }
        } else {
            editingWorkoutId = -1L;
        }
    }

    @Override
    public void onMuscleGroupClick(MuscleGroup muscleGroup) {
        selectedMuscleGroup = muscleGroup;
        showMuscleExercises(muscleGroup);
    }

    private void showMuscleExercises(MuscleGroup group) {
        ivSelectedMuscleIcon.setImageResource(group.getIconResId());
        tvSelectedMuscleTitle.setText(group.getTitle() + " Exercises");
        muscleExerciseAdapter.setExercises(group.getExercises());

        llStepAMuscleGrid.setVisibility(View.GONE);
        llStepBExercises.setVisibility(View.VISIBLE);

        nsvRoutineContent.post(new Runnable() {
            @Override
            public void run() {
                nsvRoutineContent.smoothScrollTo(0, llStepBExercises.getTop());
            }
        });
    }

    private void showMuscleGrid() {
        llStepBExercises.setVisibility(View.GONE);
        llStepAMuscleGrid.setVisibility(View.VISIBLE);
        muscleGroupAdapter.notifyDataSetChanged();
    }

    @Override
    public void onExerciseSelected(String exerciseName) {
        showExerciseConfigBottomSheet(exerciseName);
    }

    private void showAddCustomExerciseDialog() {
        if (selectedMuscleGroup == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Custom Exercise");
        builder.setMessage("Enter a custom exercise for " + selectedMuscleGroup.getTitle() + ":");

        FrameLayout container = new FrameLayout(this);
        int paddingPx = Math.round(20 * getResources().getDisplayMetrics().density);
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, 0);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("e.g. Cable Flyes");
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String customName = input.getText().toString().trim();
                if (TextUtils.isEmpty(customName)) {
                    Toast.makeText(CreateEditRoutineActivity.this, "Please enter an exercise name.", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedMuscleGroup.addExercise(customName);
                MuscleGroup.saveCustomExercise(CreateEditRoutineActivity.this, selectedMuscleGroup.getId(), customName);
                muscleExerciseAdapter.setExercises(selectedMuscleGroup.getExercises());
                muscleGroupAdapter.notifyDataSetChanged();
                showExerciseConfigBottomSheet(customName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Step C: Opens a bottom sheet with the selected exercise image, name, and Sets/Reps/Weight inputs.
     */
    private void showExerciseConfigBottomSheet(final String exerciseName) {
        final BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_exercise_config, null);
        sheetDialog.setContentView(sheetView);

        ImageView ivThumb = sheetView.findViewById(R.id.ivSheetExerciseThumb);
        TextView tvMuscleBadge = sheetView.findViewById(R.id.tvSheetMuscleBadge);
        TextView tvExerciseName = sheetView.findViewById(R.id.tvSheetExerciseName);
        final EditText etSets = sheetView.findViewById(R.id.etSheetSets);
        final EditText etReps = sheetView.findViewById(R.id.etSheetReps);
        final EditText etWeight = sheetView.findViewById(R.id.etSheetWeight);
        Button btnAdd = sheetView.findViewById(R.id.btnSheetAddExercise);

        tvMuscleBadge.setText(selectedMuscleGroup != null ? selectedMuscleGroup.getTitle() : "Exercise");
        tvExerciseName.setText(exerciseName);
        ExerciseImageHelper.getInstance().loadExerciseImage(this, exerciseName, ivThumb);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int sets;
                try {
                    sets = Integer.parseInt(etSets.getText().toString().trim());
                } catch (NumberFormatException e) {
                    sets = 0;
                }
                if (sets <= 0) {
                    etSets.setError("Enter sets >= 1");
                    etSets.requestFocus();
                    return;
                }

                int reps;
                try {
                    reps = Integer.parseInt(etReps.getText().toString().trim());
                } catch (NumberFormatException e) {
                    reps = 0;
                }
                if (reps <= 0) {
                    etReps.setError("Enter reps >= 1");
                    etReps.requestFocus();
                    return;
                }

                double weight = 0.0;
                String weightStr = etWeight.getText().toString().trim();
                if (!TextUtils.isEmpty(weightStr)) {
                    try {
                        weight = Double.parseDouble(weightStr);
                    } catch (NumberFormatException e) {
                        etWeight.setError("Invalid weight");
                        etWeight.requestFocus();
                        return;
                    }
                }
                if (weight < 0) {
                    etWeight.setError("Weight cannot be negative");
                    etWeight.requestFocus();
                    return;
                }

                pendingExercises.add(new Exercise(exerciseName, sets, reps, weight));
                refreshAddedExercisesList();
                sheetDialog.dismiss();
                Toast.makeText(CreateEditRoutineActivity.this, "Added " + exerciseName, Toast.LENGTH_SHORT).show();
            }
        });

        sheetDialog.show();
    }

    private void refreshAddedExercisesList() {
        llRoutineExercisesList.removeAllViews();
        int count = pendingExercises.size();
        tvRoutineExerciseCount.setText(count + " added");

        if (pendingExercises.isEmpty()) {
            tvRoutineNoExercises.setVisibility(View.VISIBLE);
            return;
        }

        tvRoutineNoExercises.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (final Exercise exercise : pendingExercises) {
            View chipView = inflater.inflate(R.layout.item_added_exercise_chip, llRoutineExercisesList, false);
            ImageView ivThumb = chipView.findViewById(R.id.ivAddedExerciseThumb);
            TextView tvName = chipView.findViewById(R.id.tvAddedExerciseName);
            TextView tvDetails = chipView.findViewById(R.id.tvAddedExerciseDetails);
            ImageButton btnRemove = chipView.findViewById(R.id.btnRemoveAddedExercise);

            ExerciseImageHelper.getInstance().loadExerciseImage(this, exercise.getExerciseName(), ivThumb);
            tvName.setText(exercise.getExerciseName());
            String weightText = exercise.getWeight() > 0 ? " @ " + exercise.getWeight() + " kg" : " (Bodyweight)";
            tvDetails.setText(exercise.getSets() + " sets × " + exercise.getReps() + " reps" + weightText);

            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pendingExercises.remove(exercise);
                    refreshAddedExercisesList();
                }
            });

            llRoutineExercisesList.addView(chipView);
        }
    }

    private void saveRoutine() {
        String routineName = etRoutineName.getText().toString().trim();
        if (TextUtils.isEmpty(routineName)) {
            etRoutineName.setError("Routine name is required");
            etRoutineName.requestFocus();
            return;
        }

        if (pendingExercises.isEmpty()) {
            Toast.makeText(this, "Please add at least one exercise to this routine.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingWorkoutId != -1L) {
            boolean updated = databaseHelper.updateCustomWorkout(editingWorkoutId, routineName, pendingExercises);
            if (updated) {
                Toast.makeText(this, "Routine '" + routineName + "' updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update routine. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            long userId = sessionManager.getUserId();
            long newId = databaseHelper.createCustomWorkout(userId, routineName, pendingExercises);
            if (newId != -1L) {
                Toast.makeText(this, "Routine '" + routineName + "' created!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to create routine. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleBackNavigation() {
        if (llStepBExercises.getVisibility() == View.VISIBLE) {
            showMuscleGrid();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (llStepBExercises != null && llStepBExercises.getVisibility() == View.VISIBLE) {
            showMuscleGrid();
        } else {
            super.onBackPressed();
        }
    }
}
