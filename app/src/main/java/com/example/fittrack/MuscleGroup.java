package com.example.fittrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Model representing a major muscle group with its anatomical vector drawable
 * and associated exercises, plus a catalog loader supporting custom exercises.
 */
public class MuscleGroup {

    private static final String PREF_CUSTOM_EXERCISES = "FitTrackCustomExercises";

    private final String id;
    private final String title;
    private final int iconResId;
    private final List<String> exercises;

    public MuscleGroup(String id, String title, int iconResId, List<String> exercises) {
        this.id = id;
        this.title = title;
        this.iconResId = iconResId;
        this.exercises = exercises != null ? new ArrayList<>(exercises) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public List<String> getExercises() {
        return exercises;
    }

    public void addExercise(String exerciseName) {
        if (TextUtils.isEmpty(exerciseName)) {
            return;
        }
        String trimmed = exerciseName.trim();
        for (String existing : exercises) {
            if (existing.equalsIgnoreCase(trimmed)) {
                return;
            }
        }
        exercises.add(trimmed);
    }

    /**
     * Returns all 11 major muscle groups with their built-in and user-added custom exercises.
     */
    public static List<MuscleGroup> getAllMuscleGroups(Context context) {
        List<MuscleGroup> groups = new ArrayList<>();

        groups.add(new MuscleGroup(
                "chest",
                "Chest",
                R.drawable.ic_muscle_chest,
                Arrays.asList(
                        "Flat Barbell Bench Press",
                        "Incline Dumbbell Press",
                        "Decline Bench Press",
                        "Cable Crossover",
                        "Chest Dips",
                        "Push-ups"
                )
        ));

        groups.add(new MuscleGroup(
                "back",
                "Full Back & Lats",
                R.drawable.ic_muscle_back,
                Arrays.asList(
                        "Conventional Deadlift",
                        "Pull-ups",
                        "Lat Pulldown",
                        "Barbell Bent-Over Row",
                        "Seated Cable Row",
                        "Single-Arm Dumbbell Row"
                )
        ));

        groups.add(new MuscleGroup(
                "shoulders",
                "Shoulders",
                R.drawable.ic_muscle_shoulders,
                Arrays.asList(
                        "Overhead Barbell Press",
                        "Dumbbell Shoulder Press",
                        "Lateral Raises",
                        "Rear Delt Flyes",
                        "Face Pulls",
                        "Front Raises"
                )
        ));

        groups.add(new MuscleGroup(
                "biceps",
                "Biceps",
                R.drawable.ic_muscle_biceps,
                Arrays.asList(
                        "Barbell Curl",
                        "Dumbbell Alternating Curl",
                        "Hammer Curl",
                        "Preacher Curl",
                        "Incline Dumbbell Curl",
                        "Cable Curl"
                )
        ));

        groups.add(new MuscleGroup(
                "triceps",
                "Triceps",
                R.drawable.ic_muscle_triceps,
                Arrays.asList(
                        "Tricep Rope Pushdown",
                        "EZ-Bar Skull Crushers",
                        "Overhead Dumbbell Extension",
                        "Close-Grip Bench Press",
                        "Bench Dips"
                )
        ));

        groups.add(new MuscleGroup(
                "forearms",
                "Forearms",
                R.drawable.ic_muscle_forearms,
                Arrays.asList(
                        "Wrist Curls",
                        "Reverse Wrist Curls",
                        "Reverse Barbell Curl",
                        "Farmer's Walk",
                        "Dead Hang"
                )
        ));

        groups.add(new MuscleGroup(
                "abs",
                "Abs & Core",
                R.drawable.ic_muscle_abs,
                Arrays.asList(
                        "Hanging Leg Raises",
                        "Cable Crunches",
                        "Plank",
                        "Russian Twists",
                        "Ab Wheel Rollout"
                )
        ));

        groups.add(new MuscleGroup(
                "quads",
                "Quads",
                R.drawable.ic_muscle_quads,
                Arrays.asList(
                        "Barbell Back Squat",
                        "Leg Press",
                        "Leg Extensions",
                        "Bulgarian Split Squat",
                        "Hack Squat",
                        "Walking Lunges"
                )
        ));

        groups.add(new MuscleGroup(
                "hamstrings",
                "Hamstrings",
                R.drawable.ic_muscle_hamstrings,
                Arrays.asList(
                        "Romanian Deadlift",
                        "Lying Leg Curl",
                        "Seated Leg Curl",
                        "Stiff-Legged Deadlift",
                        "Good Mornings"
                )
        ));

        groups.add(new MuscleGroup(
                "glutes",
                "Glutes",
                R.drawable.ic_muscle_glutes,
                Arrays.asList(
                        "Barbell Hip Thrust",
                        "Glute Bridge",
                        "Cable Kickbacks",
                        "Sumo Deadlift",
                        "Step-Ups"
                )
        ));

        groups.add(new MuscleGroup(
                "calves",
                "Calves",
                R.drawable.ic_muscle_calves,
                Arrays.asList(
                        "Standing Calf Raises",
                        "Seated Calf Raises",
                        "Leg Press Calf Raises",
                        "Donkey Calf Raises"
                )
        ));

        if (context != null) {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREF_CUSTOM_EXERCISES, Context.MODE_PRIVATE);
            for (MuscleGroup group : groups) {
                List<String> savedCustom = loadCustomExercises(prefs, group.getId());
                for (String customName : savedCustom) {
                    group.addExercise(customName);
                }
            }
        }

        return groups;
    }

    /**
     * Persists a user-created custom exercise under the specified muscle group.
     */
    public static void saveCustomExercise(Context context, String muscleId, String exerciseName) {
        if (context == null || TextUtils.isEmpty(muscleId) || TextUtils.isEmpty(exerciseName)) {
            return;
        }
        String trimmed = exerciseName.trim();
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_CUSTOM_EXERCISES, Context.MODE_PRIVATE);
        List<String> existing = loadCustomExercises(prefs, muscleId);
        for (String item : existing) {
            if (item.toLowerCase(Locale.US).equals(trimmed.toLowerCase(Locale.US))) {
                return;
            }
        }
        existing.add(trimmed);
        JSONArray array = new JSONArray();
        for (String item : existing) {
            array.put(item);
        }
        prefs.edit().putString(muscleId, array.toString()).apply();
    }

    private static List<String> loadCustomExercises(SharedPreferences prefs, String muscleId) {
        List<String> list = new ArrayList<>();
        String json = prefs.getString(muscleId, "");
        if (TextUtils.isEmpty(json)) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String val = array.optString(i, "").trim();
                if (!TextUtils.isEmpty(val)) {
                    list.add(val);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }
}
