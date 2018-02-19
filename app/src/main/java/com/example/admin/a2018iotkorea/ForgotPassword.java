package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class ForgotPassword extends AppCompatActivity {

    Button sendVerification;
    EditText email_forgotPW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        final EditText email_forgotPW = (EditText) findViewById(R.id.email_forgotpw);
        sendVerification = (Button) findViewById(R.id.sendVerification);
        sendVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                JSONObject session = new JSONObject();

                try {
                    session.put("email", email_forgotPW.getText());
                    String str_session = session.toString();
                    String result = null;

                    try {
                        result = new UserManagementThread(ForgotPassword.this).execute(getString(R.string.forgotPWEmail), str_session).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    JSONObject jResult = new JSONObject(result);
                    if (jResult.getBoolean("status")) {
                        Intent intent = new Intent (ForgotPassword.this, VerificationCodePage.class);
                        intent.putExtra("type", 2);
                        startActivity(intent);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
