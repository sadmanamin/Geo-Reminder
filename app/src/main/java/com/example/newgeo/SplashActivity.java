package com.example.newgeo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import pl.droidsonroids.gif.GifImageView;

public class SplashActivity extends AppCompatActivity {
    private GifImageView gifImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        gifImageView = (GifImageView) findViewById(R.id.gifimage);
        try{
            InputStream inputStream = getAssets().open("SplashOnceWhite.png");
            byte[] bytes = IOUtils.toByteArray(inputStream);
            //gifImageView.
        }
        catch (Exception e){

        }
    }
}
