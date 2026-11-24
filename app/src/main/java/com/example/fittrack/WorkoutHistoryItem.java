package com.example.fittrack;

import java.io.Serializable;

public class WorkoutHistoryItem implements Serializable {
    private long id;
    private long userId;
    private String workoutName;
    private String exerciseName;
    private int setsDone;
    private double weightUsed;
    private String date;
    private String duration;

    // Personal progress comparison fields against previous session of the same exercise
    private boolean hasPrevious;
    private double previousWeight;
    private int previousSets;

    public WorkoutHistoryItem() {
    }

    public WorkoutHistoryItem(long id, long userId, String workoutName, String exerciseName, int setsDone, double weightUsed, String date) {
        this(id, userId, workoutName, exerciseName, setsDone, weightUsed, date, "");
    }

    public WorkoutHistoryItem(long id, long userId, String workoutName, String exerciseName, int setsDone, double weightUsed, String date, String duration) {
        this.id = id;
        this.userId = userId;
        this.workoutName = workoutName;
        this.exerciseName = exerciseName;
        this.setsDone = setsDone;
        this.weightUsed = weightUsed;
        this.date = date;
        this.duration = duration;
        this.hasPrevious = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getWorkoutName() {
        return workoutName != null ? workoutName : "";
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }

    public String getExerciseName() {
        return exerciseName != null ? exerciseName : "";
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public int getSetsDone() {
        return setsDone;
    }

    public void setSetsDone(int setsDone) {
        this.setsDone = setsDone;
    }

    public double getWeightUsed() {
        return weightUsed;
    }

    public void setWeightUsed(double weightUsed) {
        this.weightUsed = weightUsed;
    }

    public String getDate() {
        return date != null ? date : "";
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDuration() {
        return duration != null ? duration : "";
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public double getPreviousWeight() {
        return previousWeight;
    }

    public void setPreviousWeight(double previousWeight) {
        this.previousWeight = previousWeight;
    }

    public int getPreviousSets() {
        return previousSets;
    }

    public void setPreviousSets(int previousSets) {
        this.previousSets = previousSets;
    }

    public double getWeightDifference() {
        return weightUsed - previousWeight;
    }

    public int getSetsDifference() {
        return setsDone - previousSets;
    }
}
