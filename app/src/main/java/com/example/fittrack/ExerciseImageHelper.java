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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class that fetches exercise demonstration image URLs from the Wger REST API
 * on a background thread, caches name-to-URL mappings, and loads thumbnails via Glide
 * with smart keyword/synonym matching and a built-in fitness vector icon fallback.
 */
public class ExerciseImageHelper {

    private static final String TAG = "ExerciseImageHelper";
    private static final String WGER_BASE_URL = "https://wger.de";
    private static final String WGER_EXERCISE_INFO_URL = "https://wger.de/api/v2/exerciseinfo/?language=2&limit=200";
    private static final String WGER_EXERCISE_IMAGE_URL = "https://wger.de/api/v2/exerciseimage/?format=json&limit=200";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final long RETRY_COOLDOWN_MS = 30000L;
    private static final int CORNER_RADIUS_DP = 8;

    private static final Set<String> NOISE_TOKENS = new HashSet<>(Arrays.asList(
            "flat", "conventional", "alternating", "single", "arm", "standing", "seated",
            "lying", "donkey", "walking", "hanging", "bulgarian", "russian", "ez", "bar",
            "barbell", "dumbbell", "cable", "rope", "machine", "smith", "band", "stiff",
            "legged", "with", "and", "the", "on", "to", "of", "in"
    ));

    private static final List<KeywordRule> KEYWORD_RULES = buildKeywordRules();

    private static volatile ExerciseImageHelper instance;

    private final Map<String, String> exerciseImageCache = new ConcurrentHashMap<>();
    private final Map<String, CachedEntry> searchableEntries = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<PendingImageLoad> pendingLoads = new ArrayList<>();

    private volatile boolean isFetching = false;
    private volatile boolean hasFetchedSuccess = false;
    private volatile long lastFetchAttemptMs = 0L;

    private static class CachedEntry {
        final String normalizedName;
        final String imageUrl;
        final boolean isMain;

        CachedEntry(String normalizedName, String imageUrl, boolean isMain) {
            this.normalizedName = normalizedName;
            this.imageUrl = imageUrl;
            this.isMain = isMain;
        }
    }

    private static class PendingImageLoad {
        final WeakReference<Context> contextRef;
        final WeakReference<ImageView> imageViewRef;
        final String normalizedKey;

        PendingImageLoad(Context context, ImageView imageView, String normalizedKey) {
            this.contextRef = new WeakReference<>(context);
            this.imageViewRef = new WeakReference<>(imageView);
            this.normalizedKey = normalizedKey;
        }
    }

    private static class KeywordRule {
        final String[] requiredAny;
        final String[] requiredAlsoAny;
        final String[] excludedAny;
        final String[] targetPhrases;

        KeywordRule(String[] requiredAny, String[] requiredAlsoAny, String[] excludedAny, String[] targetPhrases) {
            this.requiredAny = requiredAny;
            this.requiredAlsoAny = requiredAlsoAny;
            this.excludedAny = excludedAny;
            this.targetPhrases = targetPhrases;
        }

        boolean matches(String query) {
            if (requiredAny != null && !containsAnyTerm(query, requiredAny)) {
                return false;
            }
            if (requiredAlsoAny != null && !containsAnyTerm(query, requiredAlsoAny)) {
                return false;
            }
            if (excludedAny != null && containsAnyTerm(query, excludedAny)) {
                return false;
            }
            return true;
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

        showFallback(context, imageView);

        if (!hasFetchedSuccess) {
            ensureDataLoaded(new PendingImageLoad(context, imageView, normalizedKey));
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
        boolean success = false;
        Map<Integer, List<String>> exerciseIdToNames = new HashMap<>();

        try {
            String infoJson = fetchUrl(WGER_EXERCISE_INFO_URL);
            if (!TextUtils.isEmpty(infoJson)) {
                parseExerciseInfoResponse(infoJson, exerciseIdToNames);
            }

            String imageJson = fetchUrl(WGER_EXERCISE_IMAGE_URL);
            if (!TextUtils.isEmpty(imageJson)) {
                String nextUrl = parseExerciseImageResponse(imageJson, exerciseIdToNames);
                // Fetch one additional page of images if paginated so almost all Wger images are cached
                if (!TextUtils.isEmpty(nextUrl)) {
                    String nextImageJson = fetchUrl(nextUrl);
                    if (!TextUtils.isEmpty(nextImageJson)) {
                        parseExerciseImageResponse(nextImageJson, exerciseIdToNames);
                    }
                }
            }

            success = !exerciseImageCache.isEmpty();
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch exercise images from Wger API: " + e.getMessage());
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

    private String fetchUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "FitTrack-Android/1.0");

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
                return responseBuilder.toString();
            } else {
                Log.w(TAG, "Wger API returned HTTP " + responseCode + " for " + urlString);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error fetching " + urlString + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private void parseExerciseInfoResponse(String jsonString, Map<Integer, List<String>> exerciseIdToNames) {
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

                List<String> names = extractExerciseNames(exerciseObj);
                if (names.isEmpty()) {
                    continue;
                }

                int id = exerciseObj.optInt("id", -1);
                if (id > 0) {
                    exerciseIdToNames.put(id, names);
                }
                int baseId = exerciseObj.optInt("exercise_base", -1);
                if (baseId > 0) {
                    exerciseIdToNames.put(baseId, names);
                }

                JSONArray images = exerciseObj.optJSONArray("images");
                if (images != null) {
                    for (int j = 0; j < images.length(); j++) {
                        JSONObject imgObj = images.optJSONObject(j);
                        if (imgObj == null) {
                            continue;
                        }
                        String rawUrl = imgObj.optString("image", "").trim();
                        if (TextUtils.isEmpty(rawUrl)) {
                            continue;
                        }
                        String fullUrl = rawUrl.startsWith("/") ? WGER_BASE_URL + rawUrl : rawUrl;
                        boolean isMain = imgObj.optBoolean("is_main", false);
                        for (String name : names) {
                            cacheNameMapping(name, fullUrl, isMain);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing Wger exerciseinfo JSON: " + e.getMessage());
        }
    }

    private String parseExerciseImageResponse(String jsonString, Map<Integer, List<String>> exerciseIdToNames) {
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONArray results = root.optJSONArray("results");
            if (results == null) {
                return null;
            }

            for (int i = 0; i < results.length(); i++) {
                JSONObject imgObj = results.optJSONObject(i);
                if (imgObj == null) {
                    continue;
                }

                String rawUrl = imgObj.optString("image", "").trim();
                if (TextUtils.isEmpty(rawUrl)) {
                    continue;
                }
                String fullUrl = rawUrl.startsWith("/") ? WGER_BASE_URL + rawUrl : rawUrl;
                boolean isMain = imgObj.optBoolean("is_main", false);

                int exerciseBaseId = imgObj.optInt("exercise_base", -1);
                int exerciseId = imgObj.optInt("exercise", -1);

                List<String> linkedNames = exerciseIdToNames.get(exerciseBaseId);
                if (linkedNames == null || linkedNames.isEmpty()) {
                    linkedNames = exerciseIdToNames.get(exerciseId);
                }
                if (linkedNames != null) {
                    for (String name : linkedNames) {
                        cacheNameMapping(name, fullUrl, isMain);
                    }
                }

                // Also index descriptive Wger filenames (e.g. "Bench-press-1.png" -> "bench press")
                String fileDerivedName = extractNameFromImageUrl(fullUrl);
                if (!TextUtils.isEmpty(fileDerivedName)) {
                    cacheNameMapping(fileDerivedName, fullUrl, isMain);
                }
            }

            String next = root.optString("next", "").trim();
            if (!TextUtils.isEmpty(next) && !"null".equalsIgnoreCase(next)) {
                return next.startsWith("/") ? WGER_BASE_URL + next : next;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing Wger exerciseimage JSON: " + e.getMessage());
        }
        return null;
    }

    private String extractNameFromImageUrl(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            return null;
        }
        int lastSlash = imageUrl.lastIndexOf('/');
        String filename = lastSlash >= 0 ? imageUrl.substring(lastSlash + 1) : imageUrl;
        int queryIdx = filename.indexOf('?');
        if (queryIdx >= 0) {
            filename = filename.substring(0, queryIdx);
        }
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx > 0) {
            filename = filename.substring(0, dotIdx);
        }
        // Strip trailing sequence numbers like "-1", "-2", "_01"
        filename = filename.replaceAll("[-_]+\\d+$", "");
        String normalized = normalizeName(filename);
        if (normalized.length() < 3 || normalized.matches("^[0-9\\s]+$")) {
            return null;
        }
        // Ignore UUID / hex-hash filenames
        if (!normalized.contains(" ") && normalized.length() >= 16 && normalized.matches("^[0-9a-f]+$")) {
            return null;
        }
        // Ignore random hash-like single tokens with no spaces and digits mixed in
        if (!normalized.contains(" ") && normalized.matches(".*\\d.*") && normalized.length() > 10) {
            return null;
        }
        return normalized;
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

    private void cacheNameMapping(String rawName, String imageUrl, boolean isMain) {
        String normalized = normalizeName(rawName);
        if (TextUtils.isEmpty(normalized) || TextUtils.isEmpty(imageUrl)) {
            return;
        }

        CachedEntry existing = searchableEntries.get(normalized);
        if (existing == null || (isMain && !existing.isMain)) {
            searchableEntries.put(normalized, new CachedEntry(normalized, imageUrl, isMain));
            exerciseImageCache.put(normalized, imageUrl);
            String compact = normalized.replace(" ", "");
            if (!TextUtils.isEmpty(compact)) {
                exerciseImageCache.put(compact, imageUrl);
            }
        }
    }

    private String findImageUrl(String normalizedQuery) {
        if (TextUtils.isEmpty(normalizedQuery) || exerciseImageCache.isEmpty()) {
            return null;
        }

        // 1. Exact, compact, and singular/plural lookup
        String url = lookupExactOrPlural(normalizedQuery);
        if (url != null) {
            return url;
        }

        // 2. All-token match on full query
        url = matchAllTokensInEntries(normalizedQuery.split(" "));
        if (url != null) {
            return url;
        }

        // 3. Core-name match after stripping equipment/position modifiers (e.g. "Flat Barbell Bench Press" -> "bench press")
        String coreQuery = stripNoiseTokens(normalizedQuery);
        if (!TextUtils.isEmpty(coreQuery) && !coreQuery.equals(normalizedQuery)) {
            url = lookupExactOrPlural(coreQuery);
            if (url != null) {
                return url;
            }
            url = matchAllTokensInEntries(coreQuery.split(" "));
            if (url != null) {
                return url;
            }
        }

        // 4. Smart keyword & synonym rule matching
        for (KeywordRule rule : KEYWORD_RULES) {
            if (rule.matches(normalizedQuery)) {
                for (String targetPhrase : rule.targetPhrases) {
                    String matched = matchPhrase(targetPhrase);
                    if (matched != null) {
                        return matched;
                    }
                }
            }
        }

        // 5. Scored token overlap fallback across all cached Wger entries
        return findBestTokenOverlapMatch(normalizedQuery);
    }

    private String lookupExactOrPlural(String query) {
        String url = exerciseImageCache.get(query);
        if (url != null) {
            return url;
        }
        String compact = query.replace(" ", "");
        url = exerciseImageCache.get(compact);
        if (url != null) {
            return url;
        }
        if (query.endsWith("s") && query.length() > 3) {
            String singular = query.substring(0, query.length() - 1);
            url = exerciseImageCache.get(singular);
            if (url != null) {
                return url;
            }
            url = exerciseImageCache.get(singular.replace(" ", ""));
            if (url != null) {
                return url;
            }
        } else {
            String plural = query + "s";
            url = exerciseImageCache.get(plural);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private String matchPhrase(String phrase) {
        String normalizedPhrase = normalizeName(phrase);
        String url = lookupExactOrPlural(normalizedPhrase);
        if (url != null) {
            return url;
        }
        return matchAllTokensInEntries(normalizedPhrase.split(" "));
    }

    private String matchAllTokensInEntries(String[] queryTokens) {
        if (queryTokens == null || queryTokens.length == 0) {
            return null;
        }

        CachedEntry bestEntry = null;
        for (CachedEntry entry : searchableEntries.values()) {
            if (!entry.normalizedName.contains(" ") && queryTokens.length > 1) {
                continue;
            }
            if (containsAllTokens(entry.normalizedName, queryTokens)) {
                if (bestEntry == null) {
                    bestEntry = entry;
                } else if (entry.isMain != bestEntry.isMain) {
                    if (entry.isMain) {
                        bestEntry = entry;
                    }
                } else if (entry.normalizedName.length() < bestEntry.normalizedName.length()) {
                    bestEntry = entry;
                }
            }
        }
        return bestEntry != null ? bestEntry.imageUrl : null;
    }

    private String findBestTokenOverlapMatch(String normalizedQuery) {
        String[] tokens = normalizedQuery.split(" ");
        List<String> significantTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 3 && !NOISE_TOKENS.contains(token)) {
                significantTokens.add(singularizeToken(token));
            }
        }
        if (significantTokens.isEmpty()) {
            return null;
        }

        CachedEntry bestEntry = null;
        int bestScore = 0;

        for (CachedEntry entry : searchableEntries.values()) {
            int score = 0;
            for (String token : significantTokens) {
                if (entry.normalizedName.contains(token)) {
                    score += 2;
                }
            }
            if (score > 0 && entry.isMain) {
                score += 1;
            }
            if (score > bestScore || (score == bestScore && score > 0 && bestEntry != null
                    && entry.normalizedName.length() < bestEntry.normalizedName.length())) {
                bestScore = score;
                bestEntry = entry;
            }
        }
        return bestEntry != null ? bestEntry.imageUrl : null;
    }

    private String stripNoiseTokens(String normalizedQuery) {
        String[] tokens = normalizedQuery.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty() || NOISE_TOKENS.contains(token)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(token);
        }
        return sb.toString();
    }

    private boolean containsAllTokens(String candidateKey, String[] queryTokens) {
        for (String token : queryTokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (!candidateKey.contains(singularizeToken(token))) {
                return false;
            }
        }
        return true;
    }

    private String singularizeToken(String token) {
        if (token.endsWith("s") && token.length() > 3) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private static boolean containsAnyTerm(String text, String[] terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static List<KeywordRule> buildKeywordRules() {
        List<KeywordRule> rules = new ArrayList<>();
        // Specific multi-concept rules first so they are not shadowed by broader rules
        rules.add(new KeywordRule(
                new String[]{"incline"},
                new String[]{"curl"},
                null,
                new String[]{"incline dumbbell curl", "incline curl", "dumbbell curl", "bicep curl", "curl"}));
        rules.add(new KeywordRule(
                new String[]{"incline"},
                new String[]{"press", "bench", "chest"},
                null,
                new String[]{"incline bench press", "incline dumbbell press", "incline press", "bench press"}));
        rules.add(new KeywordRule(
                new String[]{"decline"},
                null,
                null,
                new String[]{"decline bench press", "decline press", "bench press"}));
        rules.add(new KeywordRule(
                new String[]{"close"},
                new String[]{"bench", "press"},
                null,
                new String[]{"close grip bench press", "bench press", "triceps"}));
        rules.add(new KeywordRule(
                new String[]{"pushdown", "push down"},
                null,
                null,
                new String[]{"tricep pushdown", "pushdown", "triceps extension", "cable extension"}));
        rules.add(new KeywordRule(
                new String[]{"push up", "pushup", "push"},
                null,
                new String[]{"pushdown", "push down"},
                new String[]{"push up", "push ups", "pushup", "pushups", "push"}));
        rules.add(new KeywordRule(
                new String[]{"pulldown", "pull down"},
                null,
                null,
                new String[]{"lat pulldown", "pulldown", "pull down"}));
        rules.add(new KeywordRule(
                new String[]{"face"},
                new String[]{"pull"},
                null,
                new String[]{"face pull", "rear delt", "cable row", "row"}));
        rules.add(new KeywordRule(
                new String[]{"pull up", "pullup", "chin up", "chinup", "dead hang"},
                null,
                null,
                new String[]{"pull up", "pull ups", "pullup", "chin up", "lat pulldown"}));
        rules.add(new KeywordRule(
                new String[]{"leg", "hamstring"},
                new String[]{"curl"},
                null,
                new String[]{"leg curl", "lying leg curl", "seated leg curl", "hamstring curl"}));
        rules.add(new KeywordRule(
                new String[]{"wrist", "forearm"},
                new String[]{"curl"},
                null,
                new String[]{"wrist curl", "forearm curl", "reverse curl", "barbell curl", "curl"}));
        rules.add(new KeywordRule(
                new String[]{"hammer"},
                new String[]{"curl"},
                null,
                new String[]{"hammer curl", "dumbbell curl", "bicep curl", "curl"}));
        rules.add(new KeywordRule(
                new String[]{"preacher"},
                new String[]{"curl"},
                null,
                new String[]{"preacher curl", "scott curl", "barbell curl", "curl"}));
        rules.add(new KeywordRule(
                new String[]{"curl"},
                null,
                new String[]{"leg", "hamstring", "wrist"},
                new String[]{"bicep curl", "barbell curl", "dumbbell curl", "cable curl", "curl"}));
        rules.add(new KeywordRule(
                new String[]{"leg"},
                new String[]{"extension"},
                null,
                new String[]{"leg extension", "leg extensions", "quad extension"}));
        rules.add(new KeywordRule(
                new String[]{"skull", "french press", "tricep", "extension"},
                null,
                new String[]{"leg"},
                new String[]{"skull crusher", "french press", "triceps extension", "tricep extension", "overhead extension", "extension"}));
        rules.add(new KeywordRule(
                new String[]{"calf"},
                null,
                null,
                new String[]{"calf raise", "calf raises", "standing calf raise", "seated calf raise", "calf"}));
        rules.add(new KeywordRule(
                new String[]{"lateral"},
                new String[]{"raise"},
                null,
                new String[]{"lateral raise", "side lateral raise", "lateral raises"}));
        rules.add(new KeywordRule(
                new String[]{"front"},
                new String[]{"raise"},
                null,
                new String[]{"front raise", "front raises", "lateral raise"}));
        rules.add(new KeywordRule(
                new String[]{"leg", "knee", "hanging"},
                new String[]{"raise"},
                null,
                new String[]{"hanging leg raise", "leg raise", "leg raises", "crunch"}));
        rules.add(new KeywordRule(
                new String[]{"raise"},
                null,
                null,
                new String[]{"lateral raise", "calf raise", "raise"}));
        rules.add(new KeywordRule(
                new String[]{"leg press"},
                null,
                null,
                new String[]{"leg press", "squat"}));
        rules.add(new KeywordRule(
                new String[]{"squat"},
                null,
                null,
                new String[]{"squat", "squats", "barbell squat", "back squat", "split squat"}));
        rules.add(new KeywordRule(
                new String[]{"lunge", "step up"},
                null,
                null,
                new String[]{"lunges", "lunge", "walking lunge", "step up", "split squat", "squat"}));
        rules.add(new KeywordRule(
                new String[]{"romanian", "stiff"},
                new String[]{"deadlift"},
                null,
                new String[]{"romanian deadlift", "stiff legged deadlift", "deadlift", "deadlifts"}));
        rules.add(new KeywordRule(
                new String[]{"deadlift", "good morning"},
                null,
                null,
                new String[]{"deadlift", "deadlifts", "romanian deadlift"}));
        rules.add(new KeywordRule(
                new String[]{"row"},
                null,
                null,
                new String[]{"bent over row", "barbell row", "cable row", "dumbbell row", "row"}));
        rules.add(new KeywordRule(
                new String[]{"bench"},
                new String[]{"press"},
                null,
                new String[]{"bench press", "barbell bench press", "dumbbell bench press"}));
        rules.add(new KeywordRule(
                new String[]{"shoulder", "overhead", "military"},
                new String[]{"press"},
                null,
                new String[]{"shoulder press", "overhead press", "military press", "dumbbell press"}));
        rules.add(new KeywordRule(
                new String[]{"dip"},
                null,
                null,
                new String[]{"dips", "dip", "chest dip", "tricep dip"}));
        rules.add(new KeywordRule(
                new String[]{"rear"},
                new String[]{"fly", "delt"},
                null,
                new String[]{"rear delt fly", "reverse fly", "butterfly", "fly"}));
        rules.add(new KeywordRule(
                new String[]{"fly", "crossover"},
                null,
                null,
                new String[]{"cable crossover", "dumbbell fly", "chest fly", "butterfly", "flyes", "fly"}));
        rules.add(new KeywordRule(
                new String[]{"hip thrust", "glute", "bridge", "kickback"},
                null,
                null,
                new String[]{"hip thrust", "glute bridge", "bridge", "kickback", "squat"}));
        rules.add(new KeywordRule(
                new String[]{"plank"},
                null,
                null,
                new String[]{"plank", "side plank", "crunch"}));
        rules.add(new KeywordRule(
                new String[]{"twist", "rollout", "ab wheel"},
                null,
                null,
                new String[]{"russian twist", "ab wheel", "crunch", "plank"}));
        rules.add(new KeywordRule(
                new String[]{"crunch", "sit up", "situp", "abs"},
                null,
                null,
                new String[]{"crunches", "crunch", "sit up", "abdominal"}));
        rules.add(new KeywordRule(
                new String[]{"farmer", "shrug"},
                null,
                null,
                new String[]{"shrug", "shrugs", "farmers walk", "deadlift"}));
        rules.add(new KeywordRule(
                new String[]{"press"},
                null,
                null,
                new String[]{"bench press", "shoulder press", "leg press", "press"}));
        return rules;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = name.toLowerCase(Locale.US)
                .replace("'", "")
                .replace("’", "")
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
