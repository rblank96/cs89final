package com.example.finaltest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCallback;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.RecognizeCallback;
import com.ibm.watson.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.tone_analyzer.v3.model.ToneScore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private SpeechToText speechService;
    private boolean listening = false;
    private MicrophoneInputStream capture;
    private ImageButton btnRecord;
    private EditText inputMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        Button btnRecord = (Button) findViewById(R.id.btn_record);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//        NavigationUI.setupWithNavController(navView, navController);

        JSONObject credentials = null; // Convert the file into a JSON object
        try {
            credentials = new JSONObject(IOUtils.toString(
                    getResources().openRawResource(R.raw.credentials), "UTF-8"
            ));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Extract the two values
        String apikey = null;
        apikey = "h-vxQxhalNNs8qWI2iNnLa8vF2cT2Vo3FCA2Lymp20L3";
//        try {
//            apikey = credentials.getString("apikey");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        Authenticator authenticator = new IamAuthenticator(apikey);
        final ToneAnalyzer toneAnalyzer = new ToneAnalyzer("2017-09-21", authenticator);

        Button analyzeButton = (Button) findViewById(R.id.analyze_button);

        btnRecord.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick (View v){
                recordMessage();
            }
        });

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // More code here
                EditText userInput = (EditText) findViewById(R.id.user_input);

                final String textToAnalyze = userInput.getText().toString();
                ToneOptions options = new ToneOptions.Builder()
                        .addTone(ToneOptions.Tone.EMOTION)
                        .html(String.valueOf(false)).build();

                ToneOptions toneOptions = new ToneOptions.Builder().text(textToAnalyze).build();
                toneAnalyzer.tone(toneOptions).enqueue(
                        new ServiceCallback<ToneAnalysis>() {
                            @Override
                            public void onResponse(Response<ToneAnalysis> response) {
                                List<ToneScore> scores = response.getResult().getDocumentTone().getTones();
                                String detectedTones = "";
                                for (ToneScore score : scores) {
                                    if (score.getScore() > 0.5f) {
                                        detectedTones += score.getToneName() + " ";
                                    }
                                }

                                final String toastMessage =
                                        "The following emotions were detected:\n\n"
                                                + detectedTones.toUpperCase();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getBaseContext(),
                                                toastMessage, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                e.printStackTrace();
                            }
                        });
            }


        });

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            makeRequest();
        }


    }

    // Speech-to-Text Record Audio permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {
                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }
        }
        if (!permissionToRecordAccepted) finish();
    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_REQUEST_CODE);
    }

    //Record a message via Watson Speech to Text
    private void recordMessage() {
        //mic.setEnabled(false);
        Authenticator authenticator = new IamAuthenticator("adfdsafd");
        speechService = new SpeechToText("2017-09-21", authenticator);
        if (listening != true) {
            capture = new MicrophoneInputStream(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        speechService.recognizeUsingWebSocket(getRecognizeOptions(), new MicrophoneRecognizeDelegate());
                    } catch (Exception e) {
                        showError(e);
                    }
                }
            }).start();
            listening = true;
            Toast.makeText(MainActivity.this, "Listening....Click to Stop", Toast.LENGTH_LONG).show();
        } else {
            try {
                capture.close();
                listening = false;
                Toast.makeText(MainActivity.this, "Stopped Listening....Click to Start", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //Private Methods - Speech to Text
    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder()
                .contentType(ContentType.OPUS.toString())
                //.model("en-UK_NarrowbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }
    //Watson Speech to Text Methods.
    private class MicrophoneRecognizeDelegate implements RecognizeCallback {
        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            System.out.println(speechResults);
            if(speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showMicText(text);
            }
        }


        @Override public void onConnected() {
        }
        @Override public void onError(Exception e) {
            showError(e);
            enableMicButton();
        }
        @Override public void onDisconnected() {
            enableMicButton();
        }

        /**
         * Called when there is an inactivity timeout.
         *
         * @param runtimeException the runtime exception
         */
        @Override
        public void onInactivityTimeout(RuntimeException runtimeException) {

        }

        /**
         * Called when the service is listening for audio.
         */
        @Override
        public void onListening() {

        }

        /**
         * Called after the service returns the final result for the transcription.
         */
        @Override
        public void onTranscriptionComplete() {

        }
    }
    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                inputMessage.setText(text);
            }
        });
    }
    private void enableMicButton() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                btnRecord.setEnabled(true);
            }
        });
    }
    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }



};
