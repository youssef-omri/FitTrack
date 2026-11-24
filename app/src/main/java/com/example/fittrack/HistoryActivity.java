package com.example.fittrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private ImageButton btnHistoryBack;
    private TextView tvTotalSessionsCount;
    private TextView tvTotalExercisesCount;
    private TextView tvTotalProgressCount;
    private LinearLayout llHistorySummaryBar;
    private LinearLayout llHistoryEmptyState;
    private Button btnStartFirstWorkout;
    private RecyclerView rvWorkoutHistory;

    private List<HistoryDateGroup> dateGroupList;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_history);

        databaseHelper = new DatabaseHelper(this);

        btnHistoryBack = findViewById(R.id.btnHistoryBack);
        tvTotalSessionsCount = findViewById(R.id.tvTotalSessionsCount);
        tvTotalExercisesCount = findViewById(R.id.tvTotalExercisesCount);
        tvTotalProgressCount = findViewById(R.id.tvTotalProgressCount);
        llHistorySummaryBar = findViewById(R.id.llHistorySummaryBar);
        llHistoryEmptyState = findViewById(R.id.llHistoryEmptyState);
        btnStartFirstWorkout = findViewById(R.id.btnStartFirstWorkout);
        rvWorkoutHistory = findViewById(R.id.rvWorkoutHistory);

        btnHistoryBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnStartFirstWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        dateGroupList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(this, dateGroupList);
        rvWorkoutHistory.setLayoutManager(new LinearLayoutManager(this));
        rvWorkoutHistory.setAdapter(historyAdapter);

        loadWorkoutHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWorkoutHistory();
    }

    private void loadWorkoutHistory() {
        long userId = sessionManager.getUserId();
        List<WorkoutHistoryItem> historyItems = databaseHelper.getUserWorkoutHistoryWithProgress(userId);

        dateGroupList.clear();

        if (historyItems == null || historyItems.isEmpty()) {
            llHistorySummaryBar.setVisibility(View.GONE);
            rvWorkoutHistory.setVisibility(View.GONE);
            llHistoryEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        llHistoryEmptyState.setVisibility(View.GONE);
        llHistorySummaryBar.setVisibility(View.VISIBLE);
        rvWorkoutHistory.setVisibility(View.VISIBLE);

        // Group history records by date (maintaining descending order)
        Map<String, HistoryDateGroup> groupMap = new LinkedHashMap<>();
        int progressGainsCount = 0;

        for (WorkoutHistoryItem item : historyItems) {
            String dateKey = item.getDate();
            if (dateKey.length() >= 10) {
                // If timestamp format like "2026-11-24 14:00", take date portion
                dateKey = dateKey.substring(0, 10);
            }

            if (!groupMap.containsKey(dateKey)) {
                groupMap.put(dateKey, new HistoryDateGroup(dateKey));
            }
            groupMap.get(dateKey).addItem(item);

            // Track progress gains (higher weight or higher sets)
            if (item.hasPrevious()) {
                if (item.getWeightDifference() > 0.001 || (Math.abs(item.getWeightDifference()) < 0.001 && item.getSetsDifference() > 0)) {
                    progressGainsCount++;
                }
            }
        }

        dateGroupList.addAll(groupMap.values());
        historyAdapter.notifyDataSetChanged();

        // Update summary metrics
        tvTotalSessionsCount.setText(String.valueOf(dateGroupList.size()));
        tvTotalExercisesCount.setText(String.valueOf(historyItems.size()));
        tvTotalProgressCount.setText(String.valueOf(progressGainsCount));
    }
}
