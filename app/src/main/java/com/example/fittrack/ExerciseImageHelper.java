package com.example.fittrack;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class that fetches exercise demonstration image URLs from the Wger REST API
 * on a background thread, caches name-to-URL mappings, and loads thumbnails via Glide
 * with a built-in fitness vector icon fallback.
 */
public class ExerciseImageHelper {

    private static final String TAG = "ExerciseImageHelper";
    private static final String WGER_BASE_URL = "https://wger.de";
    private static final String WGER_API_URL = "https://wger.de/api/v2/exerciseinfo/?language=2&limit=50";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final long RETRY_COOLDOWN_MS = 30000L;
    private static final int CORNER_RADIUS_DP = 8;

    private static volatile ExerciseImageHelper instance;

    private final Map<String, String> exerciseImageCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<PendingImageLoad> pendingLoads = new ArrayList<>();

    private volatile boolean isFetching = false;
    private volatile boolean hasFetchedSuccess = false;
    private volatile long lastFetchAttemptMs = 0L;

    private static class PendingImageLoad {
        final WeakReference<Context> contextRef;
        final WeakReference<ImageView> imageViewRef;
        final String exerciseName;
        final String normalizedKey;

        PendingImageLoad(Context context, ImageView imageView, String exerciseName, String normalizedKey) {
            this.contextRef = new WeakReference<>(context);
            this.imageViewRef = new WeakReference<>(imageView);
            this.exerciseName = exerciseName;
            this.normalizedKey = normalizedKey;
        }
    }

    private ExerciseImageHelper() {
    }

    public static ExerciseImageHelper getInstance() {
        if (instance == null) {
            synchronized (ExerciseImageHelper.class) {
                if (instance == null) {
                    instance = new ExerciseImageHelper();
                }
            }
        }
        return instance;
    }

    /**
     * Triggers an asynchronous prefetch of Wger exercise images if not already fetched.
     */
    public void prefetchExercises(Context context) {
        if (context == null) {
            return;
        }
        ensureDataLoaded(null);
    }

    /**
     * Loads the demonstration thumbnail for the given exercise name into the target ImageView.
     * Falls back to R.drawable.ic_fitness_center when offline or when no matching image exists.
     */
    public void loadExerciseImage(Context context, String exerciseName, ImageView imageView) {
        if (!isContextValid(context) || imageView == null) {
            return;
        }

        String normalizedKey = normalizeName(exerciseName);
        imageView.setTag(R.id.tag_exercise_image_key, normalizedKey);

        if (TextUtils.isEmpty(normalizedKey)) {
            showFallback(context, imageView);
            return;
        }

        String cachedUrl = findImageUrl(normalizedKey);
        if (!TextUtils.isEmpty(cachedUrl)) {
            loadWithGlide(context, cachedUrl, imageView);
            return;
        }

        // Show clean vector icon while loading or if unmatched
        showFallback(context, imageView);

        // If we haven't successfully fetched yet, fetch on a background thread and update when ready
        if (!hasFetchedSuccess) {
            ensureDataLoaded(new PendingImageLoad(context, imageView, exerciseName, normalizedKey));
        }
    }

    private void ensureDataLoaded(PendingImageLoad pendingLoad) {
        long now = System.currentTimeMillis();
        boolean shouldStartFetch = false;

        synchronized (pendingLoads) {
            if (pendingLoad != null) {
                pendingLoads.add(pendingLoad);
            }
            if (!hasFetchedSuccess && !isFetching && (now - lastFetchAttemptMs >= RETRY_COOLDOWN_MS || lastFetchAttemptMs == 0L)) {
                isFetching = true;
                lastFetchAttemptMs = now;
                shouldStartFetch = true;
            }
        }

        if (shouldStartFetch) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    fetchAndCacheFromWger();
                }
            });
        }
    }

    private void fetchAndCacheFromWger() {
        HttpURLConnection connection = null;
        boolean success = false;

        try {
            URL url = new URL(WGER_API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }
                parseAndCacheResponse(responseBuilder.toString());
                success = true;
            } else {
                Log.w(TAG, "Wger API returned HTTP " + responseCode);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch exercise images from Wger API (offline or unreachable): " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        hasFetchedSuccess = success;
        isFetching = false;

        final List<PendingImageLoad> toProcess;
        synchronized (pendingLoads) {
            toProcess = new ArrayList<>(pendingLoads);
            pendingLoads.clear();
        }

        if (success && !toProcess.isEmpty()) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (PendingImageLoad pending : toProcess) {
                        Context ctx = pending.contextRef.get();
                        ImageView iv = pending.imageViewRef.get();
                        if (!isContextValid(ctx) || iv == null) {
                            continue;
                        }
                        Object currentTag = iv.getTag(R.id.tag_exercise_image_key);
                        if (currentTag != null && currentTag.equals(pending.normalizedKey)) {
                            String url = findImageUrl(pending.normalizedKey);
                            if (!TextUtils.isEmpty(url)) {
                                loadWithGlide(ctx, url, iv);
                            }
                        }
                    }
                }
            });
        }
    }

    private void parseAndCacheResponse(String jsonString) {
        if (TextUtils.isEmpty(jsonString)) {
            return;
        }
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONArray results = root.optJSONArray("results");
            if (results == null) {
                return;
            }

            for (int i = 0; i < results.length(); i++) {
                JSONObject exerciseObj = results.optJSONObject(i);
                if (exerciseObj == null) {
                    continue;
                }

                String imageUrl = extractBestImageUrl(exerciseObj.optJSONArray("images"));
                if (TextUtils.isEmpty(imageUrl)) {
                    continue;
                }

                List<String> names = extractExerciseNames(exerciseObj);
                for (String name : names) {
                    cacheNameMapping(name, imageUrl);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing Wger JSON response: " + e.getMessage());
        }
    }

    private String extractBestImageUrl(JSONArray images) {
        if (images == null || images.length() == 0) {
            return null;
        }
        String fallbackUrl = null;
        for (int i = 0; i < images.length(); i++) {
            JSONObject imgObj = images.optJSONObject(i);
            if (imgObj == null) {
                continue;
            }
            String rawUrl = imgObj.optString("image", "").trim();
            if (TextUtils.isEmpty(rawUrl)) {
                continue;
            }
            String fullUrl = rawUrl.startsWith("/") ? WGER_BASE_URL + rawUrl : rawUrl;
            if (imgObj.optBoolean("is_main", false)) {
                return fullUrl;
            }
            if (fallbackUrl == null) {
                fallbackUrl = fullUrl;
            }
        }
        return fallbackUrl;
    }

    private List<String> extractExerciseNames(JSONObject exerciseObj) {
        List<String> names = new ArrayList<>();
        String topName = exerciseObj.optString("name", "").trim();
        if (!TextUtils.isEmpty(topName)) {
            names.add(topName);
        }

        JSONArray translations = exerciseObj.optJSONArray("translations");
        if (translations != null) {
            String anyTranslationName = null;
            for (int i = 0; i < translations.length(); i++) {
                JSONObject translation = translations.optJSONObject(i);
                if (translation == null) {
                    continue;
                }
                String translatedName = translation.optString("name", "").trim();
                if (TextUtils.isEmpty(translatedName)) {
                    continue;
                }
                if (anyTranslationName == null) {
                    anyTranslationName = translatedName;
                }
                int langId = extractLanguageId(translation.opt("language"));
                if (langId == 2) {
                    names.add(translatedName);
                }
            }
            if (names.isEmpty() && !TextUtils.isEmpty(anyTranslationName)) {
                names.add(anyTranslationName);
            }
        }
        return names;
    }

    private int extractLanguageId(Object langObj) {
        if (langObj instanceof Number) {
            return ((Number) langObj).intValue();
        }
        if (langObj instanceof JSONObject) {
            return ((JSONObject) langObj).optInt("id", -1);
        }
        return -1;
    }

    private void cacheNameMapping(String rawName, String imageUrl) {
        String normalized = normalizeName(rawName);
        if (TextUtils.isEmpty(normalized)) {
            return;
        }
        exerciseImageCache.putIfAbsent(normalized, imageUrl);

        String compact = normalized.replace(" ", "");
        if (!TextUtils.isEmpty(compact)) {
            exerciseImageCache.putIfAbsent(compact, imageUrl);
        }
    }

    private String findImageUrl(String normalizedQuery) {
        if (TextUtils.isEmpty(normalizedQuery) || exerciseImageCache.isEmpty()) {
            return null;
        }

        // 1. Exact normalized match
        String url = exerciseImageCache.get(normalizedQuery);
        if (url != null) {
            return url;
        }

        // 2. Compact match (handles "pull ups" vs "pullups", "push ups" vs "pushups")
        String compactQuery = normalizedQuery.replace(" ", "");
        url = exerciseImageCache.get(compactQuery);
        if (url != null) {
            return url;
        }

        // 3. Singular / plural variants (e.g. "squats" <-> "squat", "lunges" <-> "lunge")
        if (normalizedQuery.endsWith("s") && normalizedQuery.length() > 3) {
            String singular = normalizedQuery.substring(0, normalizedQuery.length() - 1);
            url = exerciseImageCache.get(singular);
            if (url != null) {
                return url;
            }
            url = exerciseImageCache.get(singular.replace(" ", ""));
            if (url != null) {
                return url;
            }
        } else {
            String plural = normalizedQuery + "s";
            url = exerciseImageCache.get(plural);
            if (url != null) {
                return url;
            }
        }

        // 4. Token-based match against cached Wger exercise names (e.g. "bench press" -> "barbell bench press")
        String[] queryTokens = normalizedQuery.split(" ");
        if (queryTokens.length == 0) {
            return null;
        }

        String bestMatchUrl = null;
        int bestKeyLength = Integer.MAX_VALUE;
        Iterator<Map.Entry<String, String>> iterator = exerciseImageCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String candidateKey = entry.getKey();
            if (!candidateKey.contains(" ") && queryTokens.length > 1) {
                continue;
            }
            if (containsAllTokens(candidateKey, queryTokens)) {
                if (candidateKey.length() < bestKeyLength) {
                    bestKeyLength = candidateKey.length();
                    bestMatchUrl = entry.getValue();
                }
            }
        }
        return bestMatchUrl;
    }

    private boolean containsAllTokens(String candidateKey, String[] queryTokens) {
        for (String token : queryTokens) {
            if (token.isEmpty()) {
                continue;
            }
            String singularToken = (token.endsWith("s") && token.length() > 3)
                    ? token.substring(0, token.length() - 1)
                    : token;
            if (!candidateKey.contains(singularToken)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = name.toLowerCase(Locale.US)
                .replace('-', ' ')
                .replace('_', ' ')
                .replace('/', ' ')
                .trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private void loadWithGlide(Context context, String imageUrl, ImageView imageView) {
        if (!isContextValid(context)) {
            return;
        }
        int radiusPx = dpToPx(context, CORNER_RADIUS_DP);
        Glide.with(context)
                .load(imageUrl)
                .transform(new CenterCrop(), new RoundedCorners(radiusPx))
                .placeholder(R.drawable.ic_fitness_center)
                .error(R.drawable.ic_fitness_center)
                .fallback(R.drawable.ic_fitness_center)
                .into(imageView);
    }

    private void showFallback(Context context, ImageView imageView) {
        if (isContextValid(context)) {
            Glide.with(context).clear(imageView);
        }
        imageView.setImageResource(R.drawable.ic_fitness_center);
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static boolean isContextValid(Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isFinishing() && !activity.isDestroyed();
        }
        if (context instanceof ContextWrapper) {
            return isContextValid(((ContextWrapper) context).getBaseContext());
        }
        return true;
    }
}
