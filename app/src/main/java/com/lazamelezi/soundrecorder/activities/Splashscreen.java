package com.lazamelezi.soundrecorder.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lazamelezi.soundrecorder.R;

public class Splashscreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.SplashScreenTheme);
        super.onCreate(savedInstanceState);
        Intent i = new Intent(Splashscreen.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}