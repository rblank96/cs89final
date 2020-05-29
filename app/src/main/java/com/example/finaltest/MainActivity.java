package com.example.finaltest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCallback;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.tone_analyzer.v3.model.ToneScore;

import androidx.appcompat.app.AppCompatActivity;
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

    public MainActivity() throws IOException, JSONException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

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

        Button analyzeButton = (Button)findViewById(R.id.analyze_button);

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // More code here
                EditText userInput = (EditText)findViewById(R.id.user_input);

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
                                for(ToneScore score:scores) {
                                    if(score.getScore() > 0.5f) {
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



    }



}
