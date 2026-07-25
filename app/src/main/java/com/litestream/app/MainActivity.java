package com.litestream.app;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView contentList;
    private EditText searchBox;
    private WebView videoPlayer;
    private List<String> allContent = new ArrayList<>();
    private List<String> filteredContent = new ArrayList<>;
    private ArrayAdapter<String> adapter;

    // THIS IS WHERE YOU PUT YOUR GIST URL
    private static final String CONTENT_URL = "https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_GIST_ID/raw/content.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        contentList = findViewById(R.id.content_list);
        searchBox = findViewById(R.id.search_box);
        videoPlayer = findViewById(R.id.video_player);

        // Setup List Adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredContent);
        contentList.setAdapter(adapter);

        // Setup Search
        searchBox.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                filterList(searchBox.getText().toString());
                return true;
            }
            return false;
        });

        // Setup Item Click (Play Video)
        contentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = filteredContent.get(position);
                String[] parts = selectedItem.split("\\|");
                if (parts.length > 1) {
                    // Hide list, show player
                    contentList.setVisibility(View.GONE);
                    searchBox.setVisibility(View.GONE);
                    videoPlayer.setVisibility(View.VISIBLE);

                    // Load Video
                    WebSettings webSettings = videoPlayer.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setAllowFileAccess(false);
                    videoPlayer.loadUrl(parts[1]);
                }
            }
        });

        // Fetch Content from Remote Source
        fetchRemoteContent();
    }

    private void fetchRemoteContent() {
        new Thread(() -> {
            try {
                URL url = new URL(CONTENT_URL);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                connection.disconnect();

                // Parse JSON
                JSONArray jsonArray = new JSONArray(result.toString());
                allContent.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String title = item.getString("title");
                    String videoUrl = item.getString("url");
                    allContent.add(title + "|" + videoUrl);
                }

                // Update UI
                runOnUiThread(() -> {
                    filteredContent.clear();
                    filteredContent.addAll(allContent);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Content Loaded: " + allContent.size() + " items", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to load content. Check URL.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void filterList(String query) {
        filteredContent.clear();
        if (query.isEmpty()) {
            filteredContent.addAll(allContent);
        } else {
            for (String item : allContent) {
                if (item.toLowerCase().contains(query.toLowerCase())) {
                    filteredContent.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (videoPlayer.getVisibility() == View.VISIBLE) {
            videoPlayer.setVisibility(View.GONE);
            contentList.setVisibility(View.VISIBLE);
            searchBox.setVisibility(View.VISIBLE);
            videoPlayer.loadUrl("about:blank");
        } else {
            super.onBackPressed();
        }
    }
}
