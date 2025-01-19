/******************************************************************************************
 * Author  : Alex
 * Date    : 2025-01-19
 * Handles user interactions, year selection, and communicates with
 * gemini API.

 ******************************************************************************************/
package com.alex.app.yearly;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.res.ResourcesCompat; // Required for loading fonts


import com.aigestudio.wheelpicker.widgets.WheelYearPicker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    private int selectedYear = 1998;
    private WheelYearPicker yearPicker;
    private TextView responseText;
    private View mainView, responseView;
    private Button confirmButton;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable typewriterRunnable;
    private boolean isTypewriterRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views
        yearPicker = findViewById(R.id.year_picker);
        responseText = findViewById(R.id.response_text);
        mainView = findViewById(R.id.main_view);
        responseView = findViewById(R.id.response_view);
        confirmButton = findViewById(R.id.confirm_button);
        yearPicker.setSelectedYear(1998);

        // Configure WheelYearPicker
        yearPicker.setYearStart(1);
        yearPicker.setYearEnd(2025);
        yearPicker.setSelectedYear(1998);

        // Apply the custom bold font to WheelYearPicker
        Typeface customFont = ResourcesCompat.getFont(this, R.font.ibmplex);
        if (customFont != null) {
            // ensure bold styling
            yearPicker.setTypeface(Typeface.create(customFont, Typeface.BOLD));
        }

        // Update selectedYear when picker changes
        yearPicker.setOnItemSelectedListener((picker, data, position) -> {
            selectedYear = (int) data; // Store the selected year
        });

        // Set button listener to send the request
        confirmButton.setOnClickListener(v -> {
            clearResponse(); // Clear previous response
            sendHttpRequest(selectedYear);
        });
    }

    @Override
    public void onBackPressed() {
        if (responseView.getVisibility() == View.VISIBLE) {
            showMainView();
        } else {
            super.onBackPressed();
        }
    }

    private void sendHttpRequest(int year) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String apiKey = BuildConfig.API_KEY;
            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Build request body
                JSONObject requestBody = new JSONObject();
                JSONArray contentsArray = new JSONArray();
                JSONObject partsObject = new JSONObject();
                JSONArray partsArray = new JSONArray();

                partsObject.put("text", "Resalta en pocas palabras el acontecimiento más importante del año "
                        + year + ". Responde de forma breve, divertida e informativa, en un párrafo máximo, "
                        + "y termina de manera contundente. Por ejemplo: ‘En 1969, Neil Armstrong dio un pequeño paso para "
                        + "el hombre y un gran salto para la humanidad al pisar la Luna. Fue el inicio de una nueva era "
                        + "en la exploración espacial");
                partsArray.put(partsObject);

                JSONObject contentsObject = new JSONObject();
                contentsObject.put("parts", partsArray);
                contentsArray.put(contentsObject);

                requestBody.put("contents", contentsArray);

                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                    writer.write(requestBody.toString());
                    writer.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray candidatesArray = jsonResponse.getJSONArray("candidates");
                    String responseTextContent = candidatesArray.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    runOnUiThread(() -> showResponse(responseTextContent));
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();

                    runOnUiThread(() -> showResponse("Error: " + errorResponse));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showResponse("Exception: " + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void showResponse(String response) {
        mainView.setVisibility(View.GONE);
        responseView.setVisibility(View.VISIBLE);
        showResponseWithTypewriterEffect(response);
    }

    private void clearResponse() {
        if (isTypewriterRunning) {
            handler.removeCallbacks(typewriterRunnable); // Only remove the typewriter effect
            isTypewriterRunning = false;
        }
        responseText.setText(""); // Clear the response text
    }

    private void showResponseWithTypewriterEffect(String response) {
        clearResponse(); // Clear any previous effect before starting a new one

        isTypewriterRunning = true; // Mark the TypeWriter effect as running
        typewriterRunnable = new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index < response.length()) {
                    responseText.append(String.valueOf(response.charAt(index)));
                    index++;
                    handler.postDelayed(this, 50); // Continue with the next character
                } else {
                    isTypewriterRunning = false; // Mark as completed
                }
            }
        };

        handler.post(typewriterRunnable); // Start the TypeWriter effect
    }

    private void showMainView() {
        responseView.setVisibility(View.GONE);
        mainView.setVisibility(View.VISIBLE);
        clearResponse(); // Clear response when navigating back to the main view
    }
}