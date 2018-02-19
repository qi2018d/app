package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class ChangePWNotSignedIn extends AppCompatActivity {

    Button changepwbtn;
    EditText newPW, confirmNewPW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pwnot_signed_in);

        newPW = (EditText) findViewById(R.id.newPW);
        confirmNewPW = (EditText) findViewById(R.id.confirmNewPW);

        changepwbtn = (Button) findViewById(R.id.changepwbtn);
        changepwbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check if new password is the same as the confirmed new password
                if (newPW.getText().equals(confirmNewPW.getText())) {
                    JSONObject session = new JSONObject();

                    try {
                        session.put("new_pw", newPW.getText());
                        String str_session = session.toString();
                        String result = null;

                        try {
                            result = new UserManagementThread(ChangePWNotSignedIn.this).execute(getString(R.string.forgotPWChange), str_session).get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                        JSONObject jResult = new JSONObject(result);
                        if (jResult.getBoolean("status")) {
                            //When the user forgets his pw and changes it, go to main page
                            Intent intent = new Intent(ChangePWNotSignedIn.this, Home.class);
                            startActivity(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(ChangePWNotSignedIn.this, "New password does not match the confirmed password", Toast.LENGTH_SHORT);
                }
            }
        });
    }
}
