package com.litestream.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private ListView contentList;
    private EditText searchBox;
    private WebView videoPlayer;
    private List<String> allContent = new ArrayList<>();
    private List<String> filteredContent = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // ---------------------------------------------------------
    // STEP 5: PASTE YOUR GIST RAW URL BELOW THIS LINE
    private static final String CONTENT_URL = "https://gist.githubusercontent.com/pvpthekingof-dot/21ceaed39d1db8b654b2a431924add01/raw/40bafc9181b402a0462e00dac678f094f3edf270/content.json";
    // ---------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentList = findViewById(R.id.content_list);
        searchBox = findViewById(R.id.search_box);
        videoPlayer = findViewById(R.id.video_player);

        // Custom adapter forces white text so rows are visible on black background
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filteredContent) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(0xFFFFFFFF);
                text.setBackgroundColor(0xFF000000);
                text.setPadding(16, 24, 16, 24);
                return view;
            }
        };
        contentList.setAdapter(adapter);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        contentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = filteredContent.get(position);
                String[] parts = selectedItem.split("\\|");
                if (parts.length > 1) {
                    playVideo(parts[1]);
                }
            }
        });

        if (CONTENT_URL.isEmpty() || CONTENT_URL.contains("PASTE_YOUR_GIST_RAW_URL_HERE")) {
            Toast.makeText(this, "ERROR: You must update the CONTENT_URL in the code!", Toast.LENGTH_LONG).show();
        } else {
            fetchRemoteContent();
        }
    }

    private void playVideo(String videoUrl) {
        contentList.setVisibility(View.GONE);
        searchBox.setVisibility(View.GONE);
        videoPlayer.setVisibility(View.VISIBLE);

        WebSettings webSettings = videoPlayer.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // let video autoplay

        videoPlayer.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "Playback error: invalid or unreachable link", Toast.LENGTH_SHORT).show();
            }
        });

        // Wrap the raw video URL in a minimal HTML5 <video> tag — WebView.loadUrl()
        // on a raw .mp4 often fails to play inline, this is the reliable way.
        String html = "<html><body style='margin:0;padding:0;background:#000;'>"
                + "<video width='100%' height='100%' controls autoplay playsinline src='"
                + videoUrl + "'></video></body></html>";
        videoPlayer.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
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

                JSONArray jsonArray = new JSONArray(result.toString());
                allContent.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String title = item.getString("title");
                    String videoUrl = item.getString("url");
                    allContent.add(title + "|" + videoUrl);
                }

                runOnUiThread(() -> {
                    filteredContent.clear();
                    filteredContent.addAll(allContent);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Content Loaded: " + allContent.size() + " items", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to load content. Check URL and Internet.", Toast.LENGTH_LONG).show());
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
            videoPlayer.loadUrl("about:blank");
            videoPlayer.setVisibility(View.GONE);
            contentList.setVisibility(View.VISIBLE);
            searchBox.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}
