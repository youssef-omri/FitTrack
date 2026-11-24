package com.example.fittrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom ArrayAdapter for the exercise AutoCompleteTextView dropdown that displays
 * each exercise's demonstration thumbnail beside its name.
 */
public class ExerciseDropdownAdapter extends ArrayAdapter<String> {

    private final LayoutInflater inflater;

    public ExerciseDropdownAdapter(@NonNull Context context, @NonNull String[] exercises) {
        super(context, R.layout.item_exercise_dropdown, R.id.tvDropdownExerciseName, exercises);
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.item_exercise_dropdown, parent, false);
            holder = new ViewHolder();
            holder.ivThumb = view.findViewById(R.id.ivDropdownExerciseThumb);
            holder.tvName = view.findViewById(R.id.tvDropdownExerciseName);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        String exerciseName = getItem(position);
        holder.tvName.setText(exerciseName != null ? exerciseName : "");
        ExerciseImageHelper.getInstance().loadExerciseImage(getContext(), exerciseName, holder.ivThumb);

        return view;
    }

    private static class ViewHolder {
        ImageView ivThumb;
        TextView tvName;
    }
}
