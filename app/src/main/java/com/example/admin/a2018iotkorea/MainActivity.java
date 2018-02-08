package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    Button signUpButt, signInButt, forgotButt;
    EditText userEmail, userPW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signUpButt = (Button) findViewById(R.id.signUpButton);
        signInButt = (Button) findViewById(R.id.button);
        forgotButt = (Button) findViewById(R.id.forgotButt);

        userEmail = (EditText) findViewById(R.id.emailText);
        userPW = (EditText) findViewById(R.id.password);


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
                  //  String result = new UserManagementThread(MainActivity.this).execute(getString(R.string.sign_in), str_session);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

//                Intent intent = new Intent (MainActivity.this, MainPage.class);
//                startActivity(intent);
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
