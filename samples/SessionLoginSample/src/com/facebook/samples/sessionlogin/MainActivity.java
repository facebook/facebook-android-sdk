package com.facebook.samples.sessionlogin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    Button buttonLoginActivity;
    Button buttonLoginFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        buttonLoginActivity = (Button) findViewById(R.id.buttonLoginActivity);
        buttonLoginActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginUsingActivityActivity.class);
                startActivity(intent);
            }
        });

        buttonLoginFragment = (Button) findViewById(R.id.buttonLoginFragment);
        buttonLoginFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginUsingFragmentActivity.class);
                startActivity(intent);
            }
        });
    }
}