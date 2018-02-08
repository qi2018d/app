package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class ForgotPassword extends AppCompatActivity {

    Button sendVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        sendVerification = (Button) findViewById(R.id.sendVerification);
        sendVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = (Intent) new Intent(ForgotPassword.this, ChangePWNotSignedIn.class);
                startActivity(intent);
            }
        });
    }
}
