package com.example.omer.myapplication;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.omer.planegame.GamePlaneFragment;
import com.medisense.android.tiltmazes.ActivityListener;
import com.medisense.android.tiltmazes.TiltMazesFragment;

public class MainActivity extends AppCompatActivity {
    ActivityListener listener = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        int value=getIntent().getIntExtra("index",0);
        try {
            if(value==0) {
                GamePlaneFragment fragment = GamePlaneFragment.getInstance();
                getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit();
            }else{
                TiltMazesFragment fragment = TiltMazesFragment.getInstance();
                listener = fragment;
                getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return listener != null ? listener.onTouchEvent(event) : super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return listener != null ? listener.onKeyDown(keyCode, event) : super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Log.e("Mainactivity","OnBack Press "+ getSupportFragmentManager().getBackStackEntryCount());
        getSupportFragmentManager().popBackStack();
        super.onBackPressed();
    }
}
