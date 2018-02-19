package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class VerificationCodePage extends AppCompatActivity {

    Button verificationButt;
    EditText verificationCode;
    int verificationType;
    Intent activityCaller;
    String theURL, user_id;
    private SharedPreferences globalUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code_page);

        //Use "type" to see whether we're verifying for (1)signup or (2)forgotpw otherwise set to 0
        activityCaller = getIntent();
        verificationType = activityCaller.getIntExtra("type", 0);
        globalUserId = getSharedPreferences("global_user_id", 0);
        //Get string with key signup_ver_id that's inside file global_user_id
        final String user_id = globalUserId.getString("signup_ver_id", null);

        Toast.makeText(VerificationCodePage.this, String.valueOf(verificationType), Toast.LENGTH_SHORT);

        verificationCode = (EditText) findViewById(R.id.verificationCode);
        verificationButt = (Button) findViewById(R.id.verificationButt);
        verificationButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject session = new JSONObject();

                try {
                    session.put("signup_ver_id", user_id);
                    session.put("code", verificationCode.getText());
                    String str_session = session.toString();
                    String result = null;

                    //If verification is for signup
                    if (verificationType == 1) {
                        theURL = getString(R.string.signUpVerify);
                    }
                    //If verification is for forgot pw
                    else if (verificationType == 2){
                        theURL = getString(R.string.forgotPWVerify);
                    }

                    try {
                        result = new UserManagementThread(VerificationCodePage.this).execute(theURL, str_session).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    JSONObject jResult = new JSONObject(result);
                    if (jResult.getBoolean("status")) {
                        //If verifying sign up, then next page is the main home page
                        if (verificationType == 1) {
                            Intent intent = new Intent (VerificationCodePage.this, Home.class);
                            startActivity(intent);
                        }
                        //if verifying for a forgotten pw, then next page is change pw page
                        else if (verificationType == 2) {
                            Intent intent = new Intent (VerificationCodePage.this, ChangePWNotSignedIn.class);
                            startActivity(intent);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
