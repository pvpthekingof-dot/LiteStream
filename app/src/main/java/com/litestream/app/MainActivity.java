package com.litestream.app;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * LiteStream MVP
 * -----------------------------------------------------------------------
 * Single-activity, text-only streaming list built for 1GB RAM Android TV
 * boxes. No image loading libs, no ads/analytics, no heavy animations.
 *
 * Data model: LinkedHashMap<Title, Url> loaded from SAMPLE_DATA below.
 * To scale later, replace loadSampleData() with a method that fetches
 * and parses a remote JSON array of {"title":"...","url":"..."} objects
 * (e.g. using HttpURLConnection + org.json, both already in the Android
 * SDK — no extra libraries needed).
 */
public class MainActivity extends Activity {

    // ---- Full catalog: title -> direct video URL -------------------------
    // Add more entries here, or load them from a remote JSON file later.
    private static final String[][] SAMPLE_DATA = {
            {"Big Buck Bunny", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"},
            {"Elephants Dream", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"},
            {"Sintel", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"},
            {"For Bigger Blazes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"},
            {"For Bigger Escapes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"}
    };

    // Preserves insertion order, title -> url
    private final LinkedHashMap<String, String> fullCatalog = new LinkedHashMap<>();

    private EditText searchBox;
    private ListView listView;
    private VideoView videoView;
    private View headerGroup; // title + search + list, hidden during playback

    private ArrayAdapter<String> adapter;
    private ArrayList<String> visibleTitles;

    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        headerGroup = findViewById(R.id.header_group);
        searchBox = findViewById(R.id.search_box);
        listView = findViewById(R.id.content_list);
        videoView = findViewById(R.id.video_player);

        loadSampleData();

        visibleTitles = new ArrayList<>(fullCatalog.keySet());
        // Custom lightweight row (list_item.xml) is a plain white-on-black TextView,
        // so no thumbnails/images and no per-row color hacks are needed at runtime.
        adapter = new ArrayAdapter<>(this, R.layout.list_item, android.R.id.text1, visibleTitles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String title = visibleTitles.get(position);
            String url = fullCatalog.get(title);
            playVideo(title, url);
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(MainActivity.this, "Playback error: invalid or unreachable link", Toast.LENGTH_SHORT).show();
            showList();
            return true; // error handled, don't let system show its own dialog
        });

        videoView.setOnCompletionListener(mp -> showList());
    }

    /** Step 1: populate the in-memory catalog from the hardcoded sample data. */
    private void loadSampleData() {
        fullCatalog.clear();
        for (String[] entry : SAMPLE_DATA) {
            fullCatalog.put(entry[0], entry[1]);
        }
    }

    /** Step 2: real-time filter — rebuilds the visible list from the query. */
    private void filterList(String query) {
        visibleTitles.clear();
        String q = query.trim().toLowerCase();
        for (String title : fullCatalog.keySet()) {
            if (q.isEmpty() || title.toLowerCase().contains(q)) {
                visibleTitles.add(title);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /** Step 3: hide list/search, show player, and start playback. */
    private void playVideo(String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "No link available for " + title, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            headerGroup.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(Uri.parse(url));
            videoView.requestFocus();
            videoView.start();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to play: " + title, Toast.LENGTH_SHORT).show();
            showList();
        }
    }

    /** Step 4: return to the browsing list, stopping any active playback. */
    private void showList() {
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }
        videoView.setVisibility(View.GONE);
        headerGroup.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (videoView.getVisibility() == View.VISIBLE) {
            showList();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
