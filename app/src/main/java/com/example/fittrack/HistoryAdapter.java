package com.example.fittrack;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryGroupViewHolder> {

    public interface OnHistoryItemDeleteListener {
        void onDeleteHistoryItem(WorkoutHistoryItem item);
    }

    private final Context context;
    private final List<HistoryDateGroup> dateGroups;
    private final OnHistoryItemDeleteListener deleteListener;

    public HistoryAdapter(Context context, List<HistoryDateGroup> dateGroups, OnHistoryItemDeleteListener deleteListener) {
        this.context = context;
        this.dateGroups = dateGroups;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public HistoryGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history_date_group, parent, false);
        return new HistoryGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryGroupViewHolder holder, int position) {
        HistoryDateGroup group = dateGroups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return dateGroups.size();
    }

    class HistoryGroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDateGroupTitle;
        private final TextView tvDateGroupSubtitle;
        private final TextView tvDateGroupDuration;
        private final LinearLayout llDateExercisesContainer;

        public HistoryGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateGroupTitle = itemView.findViewById(R.id.tvDateGroupTitle);
            tvDateGroupSubtitle = itemView.findViewById(R.id.tvDateGroupSubtitle);
            tvDateGroupDuration = itemView.findViewById(R.id.tvDateGroupDuration);
            llDateExercisesContainer = itemView.findViewById(R.id.llDateExercisesContainer);
        }

        public void bind(HistoryDateGroup group) {
            tvDateGroupTitle.setText(formatDateForDisplay(group.getDate()));

            int count = group.getItemCount();
            tvDateGroupSubtitle.setText(count + (count == 1 ? " exercise logged" : " exercises logged"));

            List<WorkoutHistoryItem> items = group.getItems();
            Set<String> distinctDurations = new LinkedHashSet<>();
            for (WorkoutHistoryItem item : items) {
                String d = item.getDuration().trim();
                if (!TextUtils.isEmpty(d)) {
                    distinctDurations.add(d);
                }
            }
            if (!distinctDurations.isEmpty()) {
                tvDateGroupDuration.setText("⏱ " + TextUtils.join(" • ", new ArrayList<>(distinctDurations)));
                tvDateGroupDuration.setVisibility(View.VISIBLE);
            } else {
                tvDateGroupDuration.setVisibility(View.GONE);
            }

            llDateExercisesContainer.removeAllViews();
            for (int i = 0; i < items.size(); i++) {
                final WorkoutHistoryItem item = items.get(i);
                View exerciseView = LayoutInflater.from(context).inflate(R.layout.item_history_exercise, llDateExercisesContainer, false);

                TextView tvName = exerciseView.findViewById(R.id.tvHistoryExerciseName);
                TextView tvWorkout = exerciseView.findViewById(R.id.tvHistoryWorkoutName);
                TextView tvProgressBadge = exerciseView.findViewById(R.id.tvHistoryProgressBadge);
                TextView tvPerformance = exerciseView.findViewById(R.id.tvHistoryPerformance);
                TextView tvComparisonNote = exerciseView.findViewById(R.id.tvHistoryComparisonNote);
                ImageButton btnDeleteHistoryItem = exerciseView.findViewById(R.id.btnDeleteHistoryItem);
                View divider = exerciseView.findViewById(R.id.vHistoryDivider);

                btnDeleteHistoryItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (deleteListener != null) {
                            deleteListener.onDeleteHistoryItem(item);
                        }
                    }
                });

                // Hide bottom divider for last item in date card
                if (i == items.size() - 1) {
                    divider.setVisibility(View.GONE);
                }

                tvName.setText(item.getExerciseName());
                String durationStr = item.getDuration().trim();
                if (!TextUtils.isEmpty(durationStr)) {
                    tvWorkout.setText("from " + item.getWorkoutName() + " • ⏱ " + durationStr);
                } else {
                    tvWorkout.setText("from " + item.getWorkoutName());
                }

                String weightStr = item.getWeightUsed() > 0 ? item.getWeightUsed() + " kg" : "Bodyweight";
                tvPerformance.setText(weightStr + " • " + item.getSetsDone() + " sets");

                // Highlight personal progress compared to previous session
                if (item.hasPrevious()) {
                    double weightDiff = item.getWeightDifference();
                    int setsDiff = item.getSetsDifference();

                    String prevWeightStr = item.getPreviousWeight() > 0 ? item.getPreviousWeight() + " kg" : "Bodyweight";
                    tvComparisonNote.setText("Previous: " + prevWeightStr + " (" + item.getPreviousSets() + " sets)");
                    tvComparisonNote.setVisibility(View.VISIBLE);

                    if (weightDiff > 0.001) {
                        tvProgressBadge.setText(String.format(Locale.US, "▲ +%.1f kg Progress", weightDiff));
                        tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_success);
                        tvProgressBadge.setTextColor(ContextCompat.getColor(context, R.color.accent));
                    } else if (weightDiff < -0.001) {
                        tvProgressBadge.setText(String.format(Locale.US, "▼ %.1f kg", weightDiff));
                        tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_danger);
                        tvProgressBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
                    } else {
                        // Same weight: check sets
                        if (setsDiff > 0) {
                            tvProgressBadge.setText("▲ +" + setsDiff + " sets Progress");
                            tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_success);
                            tvProgressBadge.setTextColor(ContextCompat.getColor(context, R.color.accent));
                        } else if (setsDiff < 0) {
                            tvProgressBadge.setText("▼ " + setsDiff + " sets");
                            tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_danger);
                            tvProgressBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
                        } else {
                            tvProgressBadge.setText("= Maintained Weight");
                            tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_neutral);
                            tvProgressBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                        }
                    }
                } else {
                    tvComparisonNote.setText("First logged session");
                    tvProgressBadge.setText("★ Baseline Session");
                    tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_neutral);
                    tvProgressBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
                }

                llDateExercisesContainer.addView(exerciseView);
            }
        }

        private String formatDateForDisplay(String rawDate) {
            try {
                SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsedDate = inFormat.parse(rawDate);
                if (parsedDate != null) {
                    SimpleDateFormat outFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
                    return outFormat.format(parsedDate);
                }
            } catch (Exception ignored) {
            }
            return rawDate;
        }
    }
}
