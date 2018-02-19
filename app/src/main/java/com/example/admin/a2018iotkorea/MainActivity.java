package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Button signUpButt, signInButt, forgotButt;
    EditText userEmail, userPW;
    SharedPreferences sharedPreferences;

//    class SignInAsyncTask

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signUpButt = (Button) findViewById(R.id.signUpButton);
        signInButt = (Button) findViewById(R.id.button);
        forgotButt = (Button) findViewById(R.id.forgotButt);

        userEmail = (EditText) findViewById(R.id.emailText);
        userPW = (EditText) findViewById(R.id.password);

        sharedPreferences = getSharedPreferences("global_user_id", 0);



        signUpButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        signInButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                JSONObject session = new JSONObject();

                try {
                    session.put("identifier", userEmail.getText());
                    session.put("password", userPW.getText());
                    String str_session = session.toString();
                    //Get the result from sending user email&password to the server
                    String result = null;
                    try {
                        result = new UserManagementThread(MainActivity.this).execute(getString(R.string.sign_in), str_session).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    JSONObject jResult = new JSONObject(result);
                        if (jResult.getBoolean("status")) {
                            JSONObject messageJSON = jResult.getJSONObject("message");
                            int userIDMessage = messageJSON.getInt("user_id");

                            SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
                            preferencesEditor.putInt("global_user_id", userIDMessage);
                            preferencesEditor.commit();

                            Intent intent = new Intent (MainActivity.this, Home.class);
                            startActivity(intent);
                            finish();
                        }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        forgotButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent (MainActivity.this, ForgotPassword.class);
                startActivity(intent);
            }
        });
    }
}
