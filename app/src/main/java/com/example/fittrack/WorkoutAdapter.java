package com.example.fittrack;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    public interface OnWorkoutClickListener {
        void onWorkoutClick(Workout workout);
        void onEditWorkoutClick(Workout workout);
        void onDeleteWorkoutClick(Workout workout);
    }

    private final Context context;
    private final List<Workout> workoutList;
    private final OnWorkoutClickListener listener;

    public WorkoutAdapter(Context context, List<Workout> workoutList, OnWorkoutClickListener listener) {
        this.context = context;
        this.workoutList = workoutList != null ? workoutList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workoutList.get(position);
        holder.bind(workout, listener);
    }

    @Override
    public int getItemCount() {
        return workoutList.size();
    }

    public void updateList(List<Workout> newWorkouts) {
        workoutList.clear();
        if (newWorkouts != null) {
            workoutList.addAll(newWorkouts);
        }
        notifyDataSetChanged();
    }

    class WorkoutViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWorkoutTitle;
        private final TextView tvExerciseCountBadge;
        private final ImageButton btnEditWorkout;
        private final ImageButton btnDeleteWorkout;
        private final LinearLayout llExercisePreviewList;
        private final Button btnStartWorkout;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWorkoutTitle = itemView.findViewById(R.id.tvWorkoutTitle);
            tvExerciseCountBadge = itemView.findViewById(R.id.tvExerciseCountBadge);
            btnEditWorkout = itemView.findViewById(R.id.btnEditWorkout);
            btnDeleteWorkout = itemView.findViewById(R.id.btnDeleteWorkout);
            llExercisePreviewList = itemView.findViewById(R.id.llExercisePreviewList);
            btnStartWorkout = itemView.findViewById(R.id.btnStartWorkout);
        }

        public void bind(final Workout workout, final OnWorkoutClickListener listener) {
            tvWorkoutTitle.setText(workout.getName());

            List<Exercise> exercises = workout.getExercises();
            int count = exercises != null ? exercises.size() : 0;
            tvExerciseCountBadge.setText(count + (count == 1 ? " exercise" : " exercises"));

            llExercisePreviewList.removeAllViews();
            if (exercises != null && !exercises.isEmpty()) {
                for (Exercise exercise : exercises) {
                    TextView tvExercise = new TextView(context);
                    String weightStr = exercise.getWeight() > 0 ? " @ " + exercise.getWeight() + " kg" : "";
                    String text = "• " + exercise.getExerciseName() + " (" + exercise.getSets() + " sets × " + exercise.getReps() + " reps" + weightStr + ")";
                    tvExercise.setText(text);
                    tvExercise.setTextSize(13);
                    tvExercise.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    tvExercise.setPadding(0, 3, 0, 3);
                    llExercisePreviewList.addView(tvExercise);
                }
            } else {
                TextView tvEmpty = new TextView(context);
                tvEmpty.setText("No exercises added yet");
                tvEmpty.setTextSize(13);
                tvEmpty.setTypeface(null, Typeface.ITALIC);
                tvEmpty.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
                llExercisePreviewList.addView(tvEmpty);
            }

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onWorkoutClick(workout);
                    }
                }
            };

            itemView.setOnClickListener(clickListener);
            btnStartWorkout.setOnClickListener(clickListener);

            btnEditWorkout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onEditWorkoutClick(workout);
                    }
                }
            });

            btnDeleteWorkout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDeleteWorkoutClick(workout);
                    }
                }
            });
        }
    }
}
