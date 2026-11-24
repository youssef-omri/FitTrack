package com.example.fittrack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Workout implements Serializable {
    private long id;
    private long userId;
    private String name;
    private List<Exercise> exercises;

    public Workout() {
        this.exercises = new ArrayList<>();
    }

    public Workout(long id, long userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.exercises = new ArrayList<>();
    }

    public Workout(long id, long userId, String name, List<Exercise> exercises) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.exercises = exercises != null ? exercises : new ArrayList<>();
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

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Exercise> getExercises() {
        if (exercises == null) {
            exercises = new ArrayList<>();
        }
        return exercises;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public void addExercise(Exercise exercise) {
        if (this.exercises == null) {
            this.exercises = new ArrayList<>();
        }
        this.exercises.add(exercise);
    }

    public int getExerciseCount() {
        return exercises != null ? exercises.size() : 0;
    }
}
