package com.malikyasir.landlawassist.Home;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.malikyasir.landlawassist.R;

public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread thread=new Thread(){

            public void run(){
                try{
                    sleep(1000);
                }

                catch(Exception e){
                    e.printStackTrace();
                }
                finally {
                    Intent intent=new Intent(splash.this,MainActivity.class);
                    startActivity(intent);
                }
            }
        };thread.start();
    }
}



