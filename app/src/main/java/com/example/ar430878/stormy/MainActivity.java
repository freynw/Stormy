package com.example.ar430878.stormy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    private CurrentWeather mCurrentWeather;
    private GPSTracker gps;
    private double latitude=37.8267;
    private double longitude=-122.423;

    private TextView mTimeLabel;
    private TextView mTemperatureLabel;
    private TextView mHumidityValue;
    private TextView mPrecipValue;
    private TextView mSummaryLabel;
    private ImageView mIconImageView;
    private ImageView mRefreshImageView;
    private TextView mLocationLabel;
    private ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mTimeLabel = (TextView)findViewById(R.id.timeLabel);
        mTemperatureLabel = (TextView)findViewById(R.id.temperatureLabel);
        mHumidityValue = (TextView)findViewById(R.id.humidityValue);
        mPrecipValue = (TextView)findViewById(R.id.precipValue);
        mSummaryLabel = (TextView)findViewById(R.id.summaryLabel);
        mIconImageView=(ImageView)findViewById(R.id.iconImageView);
        mRefreshImageView=(ImageView)findViewById(R.id.refreshImageView);
        mLocationLabel=(TextView)findViewById(R.id.locationLabel);
        mProgressBar=(ProgressBar)findViewById(R.id.progressBar);

        mProgressBar.setVisibility(View.INVISIBLE);


        gps = new GPSTracker(MainActivity.this);
        // check if GPS enabled
        if(gps.canGetLocation()){
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            Toast.makeText(getApplicationContext(), "Your Location is  Lat: " + latitude + "Long: " + longitude, Toast.LENGTH_LONG).show();
        }else{
            gps.showSettingsAlert();
        }

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getForecast(latitude,longitude);
            }
        });

        getForecast(latitude,longitude);
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey="29e0c6c6e6e9e6a7f7656266d7945836";
        String forecastUrl="https://api.darksky.net/forecast/"+apiKey+"/"+latitude+","+longitude;

        if(isNetworkAvailable()){
            toggleRefresh();
            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder().url(forecastUrl).build();

            Call call=client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        String jsonData=response.body().string();
                        Log.d("JSON",jsonData);
                        if(response.isSuccessful()){
                            try {
                                mCurrentWeather=getCurrentDetails(jsonData);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateDisplay();
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else{
                            alertUserAboutError();
                        }
                    }
                    catch (IOException e){
                        Log.e("Main","Exception");
                    }
                }
            });
        }

    }

    private void updateDisplay() {
        mTemperatureLabel.setText(mCurrentWeather.getTemperature()+"");
        mTimeLabel.setText("At "+mCurrentWeather.getFormattedTime()+" it will be");
        mHumidityValue.setText(mCurrentWeather.getHumidity()+"");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance()+"%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());
        mLocationLabel.setText(mCurrentWeather.getTimeZone());
        Drawable drawable=getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone=forecast.getString("timezone");
        JSONObject currently=forecast.getJSONObject("currently");
        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);
        return currentWeather;
    }


    private void alertUserAboutError() {
        AlertDialogFragment dialog=new AlertDialogFragment();
        dialog.show(getFragmentManager(),"error_dialog");
    }

    private void toggleRefresh() {
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=manager.getActiveNetworkInfo();
        boolean isAvailable=false;
        if(networkInfo!=null && networkInfo.isConnected()){
            isAvailable=true;
        }
        return isAvailable;
    }
}
