package com.example.fittrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "fittrack.db";
    private static final int DATABASE_VERSION = 2;

    // Table: users
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_USERNAME = "username";
    public static final String COLUMN_USER_PASSWORD = "password";

    // Table: workouts
    public static final String TABLE_WORKOUTS = "workouts";
    public static final String COLUMN_WORKOUT_ID = "id";
    public static final String COLUMN_WORKOUT_USER_ID = "user_id";
    public static final String COLUMN_WORKOUT_NAME = "name";

    // Table: workout_exercises
    public static final String TABLE_WORKOUT_EXERCISES = "workout_exercises";
    public static final String COLUMN_EXERCISE_ID = "id";
    public static final String COLUMN_EXERCISE_WORKOUT_ID = "workout_id";
    public static final String COLUMN_EXERCISE_NAME = "exercise_name";
    public static final String COLUMN_EXERCISE_SETS = "sets";
    public static final String COLUMN_EXERCISE_REPS = "reps";
    public static final String COLUMN_EXERCISE_WEIGHT = "weight";

    // Table: workout_history
    public static final String TABLE_WORKOUT_HISTORY = "workout_history";
    public static final String COLUMN_HISTORY_ID = "id";
    public static final String COLUMN_HISTORY_USER_ID = "user_id";
    public static final String COLUMN_HISTORY_WORKOUT_NAME = "workout_name";
    public static final String COLUMN_HISTORY_EXERCISE_NAME = "exercise_name";
    public static final String COLUMN_HISTORY_SETS_DONE = "sets_done";
    public static final String COLUMN_HISTORY_WEIGHT_USED = "weight_used";
    public static final String COLUMN_HISTORY_DATE = "date";
    public static final String COLUMN_HISTORY_DURATION = "duration";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COLUMN_USER_PASSWORD + " TEXT NOT NULL"
                + ");";

        // Create workouts table
        String createWorkoutsTable = "CREATE TABLE " + TABLE_WORKOUTS + " ("
                + COLUMN_WORKOUT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_WORKOUT_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_WORKOUT_NAME + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + COLUMN_WORKOUT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE"
                + ");";

        // Create workout_exercises table
        String createWorkoutExercisesTable = "CREATE TABLE " + TABLE_WORKOUT_EXERCISES + " ("
                + COLUMN_EXERCISE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_EXERCISE_WORKOUT_ID + " INTEGER NOT NULL, "
                + COLUMN_EXERCISE_NAME + " TEXT NOT NULL, "
                + COLUMN_EXERCISE_SETS + " INTEGER NOT NULL, "
                + COLUMN_EXERCISE_REPS + " INTEGER NOT NULL, "
                + COLUMN_EXERCISE_WEIGHT + " REAL NOT NULL, "
                + "FOREIGN KEY(" + COLUMN_EXERCISE_WORKOUT_ID + ") REFERENCES " + TABLE_WORKOUTS + "(" + COLUMN_WORKOUT_ID + ") ON DELETE CASCADE"
                + ");";

        // Create workout_history table
        String createWorkoutHistoryTable = "CREATE TABLE " + TABLE_WORKOUT_HISTORY + " ("
                + COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_HISTORY_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_HISTORY_WORKOUT_NAME + " TEXT NOT NULL, "
                + COLUMN_HISTORY_EXERCISE_NAME + " TEXT NOT NULL, "
                + COLUMN_HISTORY_SETS_DONE + " INTEGER NOT NULL, "
                + COLUMN_HISTORY_WEIGHT_USED + " REAL NOT NULL, "
                + COLUMN_HISTORY_DATE + " TEXT NOT NULL, "
                + COLUMN_HISTORY_DURATION + " TEXT DEFAULT '', "
                + "FOREIGN KEY(" + COLUMN_HISTORY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE"
                + ");";

        db.execSQL(createUsersTable);
        db.execSQL(createWorkoutsTable);
        db.execSQL(createWorkoutExercisesTable);
        db.execSQL(createWorkoutHistoryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_WORKOUT_HISTORY
                    + " ADD COLUMN " + COLUMN_HISTORY_DURATION + " TEXT DEFAULT '';");
        }
    }

    /**
     * Registers a new user and pre-populates default workout templates.
     */
    public long registerUser(String username, String password) {
        if (checkUserExists(username)) {
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long userId = -1;

        try {
            ContentValues userValues = new ContentValues();
            userValues.put(COLUMN_USER_USERNAME, username.trim());
            userValues.put(COLUMN_USER_PASSWORD, password);

            userId = db.insert(TABLE_USERS, null, userValues);

            if (userId != -1) {
                // Pre-populate 3 default workout templates for the new user
                prepopulateDefaultWorkouts(db, userId);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            userId = -1;
        } finally {
            db.endTransaction();
        }

        return userId;
    }

    /**
     * Pre-populates default workouts for a registered user:
     * 1. 'Chest Day' (Bench Press, Incline Dumbbell Press, Push-ups)
     * 2. 'Back Day' (Pull-ups, Barbell Row, Lat Pulldown)
     * 3. 'Leg Day' (Squats, Lunges, Leg Press)
     */
    private void prepopulateDefaultWorkouts(SQLiteDatabase db, long userId) {
        // 1. Chest Day
        long chestDayId = insertWorkoutInternal(db, userId, "Chest Day");
        if (chestDayId != -1) {
            insertExerciseInternal(db, chestDayId, "Bench Press", 3, 10, 60.0);
            insertExerciseInternal(db, chestDayId, "Incline Dumbbell Press", 3, 10, 20.0);
            insertExerciseInternal(db, chestDayId, "Push-ups", 3, 15, 0.0);
        }

        // 2. Back Day
        long backDayId = insertWorkoutInternal(db, userId, "Back Day");
        if (backDayId != -1) {
            insertExerciseInternal(db, backDayId, "Pull-ups", 3, 8, 0.0);
            insertExerciseInternal(db, backDayId, "Barbell Row", 3, 10, 50.0);
            insertExerciseInternal(db, backDayId, "Lat Pulldown", 3, 12, 45.0);
        }

        // 3. Leg Day
        long legDayId = insertWorkoutInternal(db, userId, "Leg Day");
        if (legDayId != -1) {
            insertExerciseInternal(db, legDayId, "Squats", 4, 10, 70.0);
            insertExerciseInternal(db, legDayId, "Lunges", 3, 12, 15.0);
            insertExerciseInternal(db, legDayId, "Leg Press", 3, 12, 100.0);
        }
    }

    private long insertWorkoutInternal(SQLiteDatabase db, long userId, String workoutName) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_WORKOUT_USER_ID, userId);
        values.put(COLUMN_WORKOUT_NAME, workoutName);
        return db.insert(TABLE_WORKOUTS, null, values);
    }

    private long insertExerciseInternal(SQLiteDatabase db, long workoutId, String exerciseName, int sets, int reps, double weight) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXERCISE_WORKOUT_ID, workoutId);
        values.put(COLUMN_EXERCISE_NAME, exerciseName);
        values.put(COLUMN_EXERCISE_SETS, sets);
        values.put(COLUMN_EXERCISE_REPS, reps);
        values.put(COLUMN_EXERCISE_WEIGHT, weight);
        return db.insert(TABLE_WORKOUT_EXERCISES, null, values);
    }

    /**
     * Checks if a user already exists with the given username.
     */
    public boolean checkUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS,
                    new String[]{COLUMN_USER_ID},
                    COLUMN_USER_USERNAME + " = ?",
                    new String[]{username.trim()},
                    null, null, null);
            return (cursor != null && cursor.moveToFirst());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Authenticates user credentials.
     */
    public long authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS,
                    new String[]{COLUMN_USER_ID},
                    COLUMN_USER_USERNAME + " = ? AND " + COLUMN_USER_PASSWORD + " = ?",
                    new String[]{username.trim(), password},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            }
            return -1;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Creates a new custom workout routine with the specified exercises.
     */
    public long createCustomWorkout(long userId, String workoutName, List<Exercise> exercises) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long workoutId = -1;
        try {
            workoutId = insertWorkoutInternal(db, userId, workoutName);
            if (workoutId != -1 && exercises != null) {
                for (Exercise exercise : exercises) {
                    insertExerciseInternal(db, workoutId,
                            exercise.getExerciseName(),
                            exercise.getSets(),
                            exercise.getReps(),
                            exercise.getWeight());
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            workoutId = -1;
        } finally {
            db.endTransaction();
        }
        return workoutId;
    }

    /**
     * Updates an existing custom workout routine's name and replaces its exercises.
     */
    public boolean updateCustomWorkout(long workoutId, String workoutName, List<Exercise> exercises) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues workoutValues = new ContentValues();
            workoutValues.put(COLUMN_WORKOUT_NAME, workoutName);
            int rows = db.update(TABLE_WORKOUTS, workoutValues,
                    COLUMN_WORKOUT_ID + " = ?",
                    new String[]{String.valueOf(workoutId)});
            if (rows <= 0) {
                return false;
            }

            db.delete(TABLE_WORKOUT_EXERCISES,
                    COLUMN_EXERCISE_WORKOUT_ID + " = ?",
                    new String[]{String.valueOf(workoutId)});

            if (exercises != null) {
                for (Exercise exercise : exercises) {
                    insertExerciseInternal(db, workoutId,
                            exercise.getExerciseName(),
                            exercise.getSets(),
                            exercise.getReps(),
                            exercise.getWeight());
                }
            }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Adds an exercise to an existing workout.
     */
    public long addExerciseToWorkout(long workoutId, String exerciseName, int sets, int reps, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        return insertExerciseInternal(db, workoutId, exerciseName, sets, reps, weight);
    }

    /**
     * Get all workouts for a specific user as a Cursor.
     */
    public Cursor getUserWorkouts(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_WORKOUTS,
                null,
                COLUMN_WORKOUT_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_WORKOUT_ID + " ASC");
    }

    /**
     * Get all workouts for a specific user as a typed List of Workout objects,
     * with each workout's exercises loaded.
     */
    public List<Workout> getUserWorkoutList(long userId) {
        List<Workout> workouts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORKOUTS,
                null,
                COLUMN_WORKOUT_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_WORKOUT_ID + " ASC");

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long workoutId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_ID));
                    String workoutName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_NAME));
                    Workout workout = new Workout(workoutId, userId, workoutName);
                    workout.setExercises(getExercisesForWorkout(workoutId));
                    workouts.add(workout);
                }
            } finally {
                cursor.close();
            }
        }
        return workouts;
    }

    /**
     * Get a single workout by its ID, with its exercises loaded.
     */
    public Workout getWorkoutById(long workoutId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORKOUTS,
                null,
                COLUMN_WORKOUT_ID + " = ?",
                new String[]{String.valueOf(workoutId)},
                null, null, null);

        Workout workout = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    long userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_USER_ID));
                    String workoutName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_NAME));
                    workout = new Workout(workoutId, userId, workoutName);
                    workout.setExercises(getExercisesForWorkout(workoutId));
                }
            } finally {
                cursor.close();
            }
        }
        return workout;
    }

    /**
     * Get all exercises for a given workout as a Cursor.
     */
    public Cursor getWorkoutExercises(long workoutId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_WORKOUT_EXERCISES,
                null,
                COLUMN_EXERCISE_WORKOUT_ID + " = ?",
                new String[]{String.valueOf(workoutId)},
                null, null, COLUMN_EXERCISE_ID + " ASC");
    }

    /**
     * Get all exercises for a given workout as a typed List.
     */
    public List<Exercise> getExercisesForWorkout(long workoutId) {
        List<Exercise> exercises = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORKOUT_EXERCISES,
                null,
                COLUMN_EXERCISE_WORKOUT_ID + " = ?",
                new String[]{String.valueOf(workoutId)},
                null, null, COLUMN_EXERCISE_ID + " ASC");

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXERCISE_ID));
                    String exerciseName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXERCISE_NAME));
                    int sets = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EXERCISE_SETS));
                    int reps = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EXERCISE_REPS));
                    double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_EXERCISE_WEIGHT));

                    exercises.add(new Exercise(id, workoutId, exerciseName, sets, reps, weight));
                }
            } finally {
                cursor.close();
            }
        }
        return exercises;
    }

    /**
     * Log a workout exercise to history.
     */
    public long logWorkoutHistory(long userId, String workoutName, String exerciseName, int setsDone, double weightUsed, String date) {
        return logWorkoutHistory(userId, workoutName, exerciseName, setsDone, weightUsed, date, "");
    }

    /**
     * Log a workout exercise to history along with the total workout duration.
     */
    public long logWorkoutHistory(long userId, String workoutName, String exerciseName, int setsDone, double weightUsed, String date, String duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HISTORY_USER_ID, userId);
        values.put(COLUMN_HISTORY_WORKOUT_NAME, workoutName);
        values.put(COLUMN_HISTORY_EXERCISE_NAME, exerciseName);
        values.put(COLUMN_HISTORY_SETS_DONE, setsDone);
        values.put(COLUMN_HISTORY_WEIGHT_USED, weightUsed);
        values.put(COLUMN_HISTORY_DATE, date);
        values.put(COLUMN_HISTORY_DURATION, duration != null ? duration : "");
        return db.insert(TABLE_WORKOUT_HISTORY, null, values);
    }

    /**
     * Get workout history for a specific user as a Cursor.
     */
    public Cursor getUserWorkoutHistory(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_WORKOUT_HISTORY,
                null,
                COLUMN_HISTORY_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_HISTORY_DATE + " DESC, " + COLUMN_HISTORY_ID + " DESC");
    }

    /**
     * Get user workout history with personal progress comparison against previous sessions.
     * Chronologically calculates progress per exercise, then returns the list sorted descending.
     */
    public List<WorkoutHistoryItem> getUserWorkoutHistoryWithProgress(long userId) {
        List<WorkoutHistoryItem> chronologicalList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query history in chronological order (ASC) so we can track progression across time
        Cursor cursor = db.query(TABLE_WORKOUT_HISTORY,
                null,
                COLUMN_HISTORY_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_HISTORY_DATE + " ASC, " + COLUMN_HISTORY_ID + " ASC");

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_ID));
                    String workoutName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_WORKOUT_NAME));
                    String exerciseName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_EXERCISE_NAME));
                    int setsDone = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_SETS_DONE));
                    double weightUsed = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_WEIGHT_USED));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_DATE));
                    int durationIndex = cursor.getColumnIndex(COLUMN_HISTORY_DURATION);
                    String duration = (durationIndex >= 0 && !cursor.isNull(durationIndex))
                            ? cursor.getString(durationIndex)
                            : "";

                    chronologicalList.add(new WorkoutHistoryItem(id, userId, workoutName, exerciseName, setsDone, weightUsed, date, duration));
                }
            } finally {
                cursor.close();
            }
        }

        // Compare each exercise to its preceding session
        Map<String, WorkoutHistoryItem> lastExerciseSessionMap = new HashMap<>();
        for (WorkoutHistoryItem item : chronologicalList) {
            String key = item.getExerciseName().trim().toLowerCase();
            if (lastExerciseSessionMap.containsKey(key)) {
                WorkoutHistoryItem previousSession = lastExerciseSessionMap.get(key);
                if (previousSession != null) {
                    item.setHasPrevious(true);
                    item.setPreviousWeight(previousSession.getWeightUsed());
                    item.setPreviousSets(previousSession.getSetsDone());
                }
            }
            lastExerciseSessionMap.put(key, item);
        }

        // Reverse to display newest sessions first
        Collections.reverse(chronologicalList);
        return chronologicalList;
    }
}
