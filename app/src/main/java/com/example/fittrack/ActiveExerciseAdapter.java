package com.example.fittrack;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActiveExerciseAdapter extends RecyclerView.Adapter<ActiveExerciseAdapter.ActiveExerciseViewHolder> {

    public interface OnExerciseStatusChangeListener {
        void onExerciseCompletedChanged(Exercise exercise, boolean isCompleted);
    }

    private final Context context;
    private final List<Exercise> exerciseList;
    private final OnExerciseStatusChangeListener statusListener;

    public ActiveExerciseAdapter(Context context, List<Exercise> exerciseList, OnExerciseStatusChangeListener statusListener) {
        this.context = context;
        this.exerciseList = exerciseList;
        this.statusListener = statusListener;
    }

    @NonNull
    @Override
    public ActiveExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_exercise, parent, false);
        return new ActiveExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveExerciseViewHolder holder, int position) {
        Exercise exercise = exerciseList.get(position);
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    class ActiveExerciseViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox cbExerciseDone;
        private final ImageView ivActiveExerciseThumb;
        private final TextView tvActiveExerciseName;
        private final TextView tvTargetInfo;
        private final TextView tvDoneBadge;
        private final EditText etActiveSets;
        private final EditText etActiveWeight;
        private final View llActiveExerciseCard;

        private TextWatcher setsWatcher;
        private TextWatcher weightWatcher;

        public ActiveExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            llActiveExerciseCard = itemView.findViewById(R.id.llActiveExerciseCard);
            cbExerciseDone = itemView.findViewById(R.id.cbExerciseDone);
            ivActiveExerciseThumb = itemView.findViewById(R.id.ivActiveExerciseThumb);
            tvActiveExerciseName = itemView.findViewById(R.id.tvActiveExerciseName);
            tvTargetInfo = itemView.findViewById(R.id.tvTargetInfo);
            tvDoneBadge = itemView.findViewById(R.id.tvDoneBadge);
            etActiveSets = itemView.findViewById(R.id.etActiveSets);
            etActiveWeight = itemView.findViewById(R.id.etActiveWeight);
        }

        public void bind(final Exercise exercise) {
            // Remove previous text watchers to avoid recycling conflicts
            if (setsWatcher != null) {
                etActiveSets.removeTextChangedListener(setsWatcher);
            }
            if (weightWatcher != null) {
                etActiveWeight.removeTextChangedListener(weightWatcher);
            }
            cbExerciseDone.setOnCheckedChangeListener(null);

            tvActiveExerciseName.setText(exercise.getExerciseName());
            ExerciseImageHelper.getInstance().loadExerciseImage(context, exercise.getExerciseName(), ivActiveExerciseThumb);

            String weightStr = exercise.getWeight() > 0 ? " @ " + exercise.getWeight() + " kg" : " (Bodyweight)";
            String targetDesc = "Target: " + exercise.getSets() + " sets × " + exercise.getReps() + " reps" + weightStr;
            tvTargetInfo.setText(targetDesc);

            // Populate current values
            etActiveSets.setText(String.valueOf(exercise.getCurrentSets()));
            if (exercise.getCurrentWeight() == (long) exercise.getCurrentWeight()) {
                etActiveWeight.setText(String.format(java.util.Locale.US, "%.0f", exercise.getCurrentWeight()));
            } else {
                etActiveWeight.setText(String.format(java.util.Locale.US, "%.1f", exercise.getCurrentWeight()));
            }

            cbExerciseDone.setChecked(exercise.isCompleted());
            tvDoneBadge.setVisibility(exercise.isCompleted() ? View.VISIBLE : View.GONE);

            // Checkbox change listener
            cbExerciseDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    exercise.setCompleted(isChecked);
                    tvDoneBadge.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    if (statusListener != null) {
                        statusListener.onExerciseCompletedChanged(exercise, isChecked);
                    }
                }
            });

            // TextWatcher for Sets
            setsWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int sets = Integer.parseInt(s.toString().trim());
                        exercise.setCurrentSets(sets);
                    } catch (NumberFormatException ignored) {
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };
            etActiveSets.addTextChangedListener(setsWatcher);

            // TextWatcher for Weight
            weightWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        double weight = Double.parseDouble(s.toString().trim());
                        exercise.setCurrentWeight(weight);
                    } catch (NumberFormatException ignored) {
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };
            etActiveWeight.addTextChangedListener(weightWatcher);
        }
    }
}
