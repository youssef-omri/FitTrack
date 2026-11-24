package com.example.fittrack;

import java.io.Serializable;

public class Exercise implements Serializable {
    private long id;
    private long workoutId;
    private String exerciseName;
    private int sets;
    private int reps;
    private double weight;

    // Session-specific fields for ActiveWorkoutActivity
    private boolean isCompleted;
    private int currentSets;
    private double currentWeight;

    public Exercise() {
    }

    public Exercise(String exerciseName, int sets, int reps, double weight) {
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.currentSets = sets;
        this.currentWeight = weight;
        this.isCompleted = false;
    }

    public Exercise(long id, long workoutId, String exerciseName, int sets, int reps, double weight) {
        this.id = id;
        this.workoutId = workoutId;
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.currentSets = sets;
        this.currentWeight = weight;
        this.isCompleted = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(long workoutId) {
        this.workoutId = workoutId;
    }

    public String getExerciseName() {
        return exerciseName != null ? exerciseName : "";
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getCurrentSets() {
        return currentSets;
    }

    public void setCurrentSets(int currentSets) {
        this.currentSets = currentSets;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
    }
}
