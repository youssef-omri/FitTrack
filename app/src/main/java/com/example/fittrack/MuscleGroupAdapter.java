package com.example.fittrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter displaying the 2-column grid of anatomical muscle group cards.
 */
public class MuscleGroupAdapter extends RecyclerView.Adapter<MuscleGroupAdapter.MuscleViewHolder> {

    public interface OnMuscleGroupClickListener {
        void onMuscleGroupClick(MuscleGroup muscleGroup);
    }

    private final Context context;
    private final List<MuscleGroup> muscleGroups;
    private final OnMuscleGroupClickListener listener;

    public MuscleGroupAdapter(Context context, List<MuscleGroup> muscleGroups, OnMuscleGroupClickListener listener) {
        this.context = context;
        this.muscleGroups = muscleGroups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MuscleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_muscle_group, parent, false);
        return new MuscleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MuscleViewHolder holder, int position) {
        MuscleGroup group = muscleGroups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return muscleGroups.size();
    }

    class MuscleViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivMuscleIcon;
        private final TextView tvMuscleName;
        private final TextView tvMuscleCount;

        MuscleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMuscleIcon = itemView.findViewById(R.id.ivMuscleIcon);
            tvMuscleName = itemView.findViewById(R.id.tvMuscleName);
            tvMuscleCount = itemView.findViewById(R.id.tvMuscleCount);
        }

        void bind(final MuscleGroup group) {
            ivMuscleIcon.setImageResource(group.getIconResId());
            tvMuscleName.setText(group.getTitle());
            int count = group.getExercises().size();
            tvMuscleCount.setText(count + (count == 1 ? " exercise" : " exercises"));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onMuscleGroupClick(group);
                    }
                }
            });
        }
    }
}
