package com.volume.changer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.validator.routines.UrlValidator;

public class MainActivity extends AppCompatActivity {
    public static final String APP_PREFERENCES = "settings";
    public static final String APP_PREFERENCES_SERVER_IP = "counter";
    private SharedPreferences appSettings;
    private String serverIp = "";

    private ImageButton popupBtn;
    private TextView currentVolume;
    private TextView actionMessage;
    private EditText serverUrl;
    private Button saveBtn;
    private Button muteBtn;
    private SeekBar seekBar;
    private ImageView wifiIndicatorImage;

    private PopupWindow mPopupWindow;

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = appSettings.edit();
        editor.putString(APP_PREFERENCES_SERVER_IP, serverIp);
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = appSettings.edit();
        editor.putString(APP_PREFERENCES_SERVER_IP, serverIp);
        editor.apply();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (appSettings.contains(APP_PREFERENCES_SERVER_IP)) {
            // Получаем число из настроек
            serverIp = appSettings.getString(APP_PREFERENCES_SERVER_IP, "");
        }
    }

    private Context mContext;
    private Activity mActivity;

    private RelativeLayout mRelativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        mContext = getApplicationContext();
        mActivity = MainActivity.this;
        // Get the widgets reference from XML layout
        mRelativeLayout = findViewById(R.id.rl);

        currentVolume = findViewById(R.id.currentVolume);
        actionMessage = findViewById(R.id.actionMessage);
        saveBtn = findViewById(R.id.saveBtn);
        serverUrl = findViewById(R.id.serverUrl);
        muteBtn = findViewById(R.id.mute);
        seekBar = findViewById(R.id.seekBar);

        wifiIndicatorImage = findViewById(R.id.wifiIndicatorImage);
        if (isNetworkAvailable()) {
            wifiIndicatorImage.setBackgroundResource(R.drawable.wifi_on_24px);
        } else {
            wifiIndicatorImage.setBackgroundResource(R.drawable.wifi_off_24px);
        }

        popupBtn = findViewById(R.id.switchOffBtn);
        popupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNetworkAvailable()) {
                    actionMessage.setVisibility(View.VISIBLE);
                    actionMessage.setText("Відсутній інтернет! Увімкніть, щоб продовжити");
                    return;
                }
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                View customView = inflater.inflate(R.layout.custom_layout, null);
                mPopupWindow = new PopupWindow(customView,
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT, true);
                mPopupWindow.setElevation(5.0f);
                mPopupWindow.showAtLocation(mRelativeLayout, Gravity.CENTER, 0, 0);

                ImageButton closeButton = customView.findViewById(R.id.ib_close);
                Button okBtn = customView.findViewById(R.id.okBtn);
                final TextView timeError = customView.findViewById(R.id.timeError);
                timeError.setVisibility(View.INVISIBLE);

                final EditText timeToOff = customView.findViewById(R.id.timeToOff);
                timeToOff.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        timeError.setVisibility(View.INVISIBLE);
                        timeError.setText("");
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        timeError.setVisibility(View.INVISIBLE);
                        timeError.setText("");
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        timeError.setVisibility(View.INVISIBLE);
                        timeError.setText("");
                    }
                });
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //clear text before
                        timeError.setVisibility(View.INVISIBLE);
                        timeError.setText("");

                        if (timeToOff.getText().toString().isEmpty()) {
                            timeError.setVisibility(View.VISIBLE);
                            timeError.setText("ПК буде вимкнено через 5 сек");
                            sendGet(serverIp + "/api-turn-off?time=5");
                            mPopupWindow.dismiss();
                        } else {
                            //to do add some validation
                            try {
                                int minToOff = Integer.parseInt(timeToOff.getText().toString());
                                sendGet(serverIp + "/api-turn-off?time=" + (minToOff * 60));
                                mPopupWindow.dismiss();
                                actionMessage.setText("ПК буде вимкнено через " + minToOff + "хв.");
                            } catch (NumberFormatException e) {
                                timeError.setVisibility(View.VISIBLE);
                                timeError.setText("Введене значення не можливо перевести в час");
                            }
                        }
                    }
                });
                Button cancelBtn = customView.findViewById(R.id.cancelBtn);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPopupWindow.dismiss();
                        showWifiIndicator();
                    }
                });

                // Set a click listener for the popup window close button
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showWifiIndicator();
                        mPopupWindow.dismiss();
                    }
                });
            }

//                mPopupWindow.setElevation(5.0f);

//            mPopupWindow.showAtLocation(mRelativeLayout, Gravity.CENTER,0,0);

        });


        seekBar.setEnabled(false);
        muteBtn.setEnabled(false);

        if (appSettings.contains(APP_PREFERENCES_SERVER_IP)) {
            // check if server IP is stored in settings already
            serverIp = appSettings.getString(APP_PREFERENCES_SERVER_IP, "");
            actionMessage.setText(">" + serverIp);
            saveBtn.setVisibility(View.INVISIBLE);
            serverUrl.setVisibility(View.INVISIBLE);
            //set progress bar start value
            sendGet(serverIp + "/api-get-volume");

            seekBar.setEnabled(true);
            muteBtn.setEnabled(true);

        } else {
            saveBtn.setVisibility(View.VISIBLE);
            serverUrl.setVisibility(View.VISIBLE);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    int progressChangedValue = 0;

                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progressChangedValue = progress;
                        if (!isNetworkAvailable()) {
                            actionMessage.setVisibility(View.VISIBLE);
                            actionMessage.setText("Відсутній інтернет! Увімкніть, щоб продовжити");
                            showWifiIndicator();
                        } else {
                            sendGet(serverIp + "/api-change-volume?value=" + progress);
                        }
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (!isNetworkAvailable()) {
                            actionMessage.setVisibility(View.VISIBLE);
                            actionMessage.setText("Відсутній інтернет! Увімкніть, щоб продовжити");
                            showWifiIndicator();
                        } else {
                            Toast.makeText(MainActivity.this, progressChangedValue,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, progressChangedValue,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void clickSave(View v) {
        showWifiIndicator();
        if (serverIp.isEmpty()) {
            actionMessage.setText("Введіть IP серверу");
        }
        if (null == serverUrl || null == serverUrl.getText() || serverUrl.getText().toString().isEmpty()) {
            actionMessage.setText("Поле сервера пусте, введіть адресу серверу!");
            return;
        } else {
            serverIp = serverUrl.getText().toString();
            System.out.println(serverIp);

            //check if server URL value is correct
            UrlValidator urlValidator = new UrlValidator();
            if (!urlValidator.isValid(serverIp)) {
                //todo need to add tool tip for this
                saveBtn.setText("Зберегти");
                actionMessage.setText("Невірно вказана адреса серверу!");
                return;
            }

            sendGet(serverIp + "/api-get-volume");

            hideShowButtons(true);
            muteBtn.setEnabled(true);
            seekBar.setEnabled(true);
        }
    }

    public void clickMute(View v) {
        if (!isNetworkAvailable()) {
            actionMessage.setVisibility(View.VISIBLE);
            actionMessage.setText("Відсутній інтернет! Увімкніть, щоб продовжити");
            showWifiIndicator();
            return;
        }
        sendGet(serverIp + "/api-change-volume?value=10");
    }

    public void sendGet(final String url) {
        actionMessage.setText("");
        actionMessage.setVisibility(View.INVISIBLE);

        if (!isNetworkAvailable()) {
            actionMessage.setVisibility(View.VISIBLE);
            actionMessage.setText("Відсутній інтернет! Увімкніть, щоб продовжити");
            showWifiIndicator();
            return;
        }

        UrlValidator urlValidator = new UrlValidator();
        if (!urlValidator.isValid(serverIp)) {
            actionMessage.setText("Невірно вказана адреса серверу!");
            actionMessage.setVisibility(View.VISIBLE);
            saveBtn.setVisibility(View.VISIBLE);
            serverUrl.setVisibility(View.VISIBLE);
            return;
        }

        actionMessage.setVisibility(View.VISIBLE);
        actionMessage.setText("Зачекайте обробки запиту");
        hideShowButtons(false);

        RequestQueue mRequestQueue = Volley.newRequestQueue(this);
        // Start the queue
        mRequestQueue.start();


        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Do something with the response
                        //todo need to add tool tip for this

                        saveBtn.setVisibility(View.INVISIBLE);
                        serverUrl.setVisibility(View.INVISIBLE);

                        if (url.contains("turn-off")) {
                            actionMessage.setVisibility(View.VISIBLE);
                            if (response.equals("off")) {
                                actionMessage.setText("ПК буде вимкнено через заданий час");
                            } else {
                                actionMessage.setText("Внутрішня помилка на сервері!!!");
                            }
                        } else {
                            currentVolume.setText("Поточна гучність: " + response);
                            //erase action message and hide it
                            actionMessage.setText("");
                            actionMessage.setVisibility(View.INVISIBLE);
                        }

                        currentVolume.setVisibility(View.VISIBLE);
                        try {
                            int progress = Integer.parseInt(response);
                            if (seekBar.getProgress() != progress) {
                                seekBar.setProgress(progress);
                            }
                        } catch (NumberFormatException e) {
                            //ignore
                        }

                        //show manage buttons
                        hideShowButtons(true);
                        wifiIndicatorImage.setBackgroundResource(R.drawable.wifi_on_24px);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //todo need to add tool tip for this
                        actionMessage.setText("Виникла помилка при з'єднанні, спробуйте ще раз");
                        saveBtn.setVisibility(View.VISIBLE);
                        serverUrl.setVisibility(View.VISIBLE);
                        serverUrl.setText(serverIp);
                        showWifiIndicator();
                    }
                });

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    private void hideShowButtons(boolean show) {
        muteBtn.setEnabled(show);
        seekBar.setEnabled(show);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showWifiIndicator() {
        if (isNetworkAvailable()) {
            wifiIndicatorImage.setBackgroundResource(R.drawable.wifi_on_24px);
        } else {
            wifiIndicatorImage.setBackgroundResource(R.drawable.wifi_off_24px);
        }
    }
}
