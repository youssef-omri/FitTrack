package com.example.fittrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for Step B showing exercises for a selected muscle group
 * with demonstration thumbnails loaded via ExerciseImageHelper.
 */
public class MuscleExerciseAdapter extends RecyclerView.Adapter<MuscleExerciseAdapter.ExerciseItemViewHolder> {

    public interface OnExerciseSelectedListener {
        void onExerciseSelected(String exerciseName);
    }

    private final Context context;
    private final List<String> exercises = new ArrayList<>();
    private final OnExerciseSelectedListener listener;

    public MuscleExerciseAdapter(Context context, OnExerciseSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setExercises(List<String> newExercises) {
        exercises.clear();
        if (newExercises != null) {
            exercises.addAll(newExercises);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExerciseItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_muscle_exercise, parent, false);
        return new ExerciseItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseItemViewHolder holder, int position) {
        String exerciseName = exercises.get(position);
        holder.bind(exerciseName);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    class ExerciseItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivExerciseThumb;
        private final TextView tvExerciseName;

        ExerciseItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivExerciseThumb = itemView.findViewById(R.id.ivMuscleExerciseThumb);
            tvExerciseName = itemView.findViewById(R.id.tvMuscleExerciseName);
        }

        void bind(final String exerciseName) {
            tvExerciseName.setText(exerciseName);
            ExerciseImageHelper.getInstance().loadExerciseImage(context, exerciseName, ivExerciseThumb);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onExerciseSelected(exerciseName);
                    }
                }
            });
        }
    }
}
