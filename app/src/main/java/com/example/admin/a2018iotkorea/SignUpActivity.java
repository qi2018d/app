package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class SignUpActivity extends AppCompatActivity  {

    Button btnSignUp;
    EditText userEmail, username, userPW, userPWConfirm;
    RadioButton male, female;
    DatePicker birthday;
    String gender;

    private SharedPreferences globalUserId;

    //TODO - check if password and confirm password are equal

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        //Create a new file named global_user_id in SharedPreferences object
        globalUserId = getSharedPreferences("global_user_id", 0);

        //Get the user's gender
        RadioGroup radiogroup = (RadioGroup) findViewById(R.id.radiobuttonsGender);
        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                male = (RadioButton) findViewById(R.id.guy);
                female = (RadioButton) findViewById(R.id.girl);
                if (male.isChecked()) {
                    gender = "1"; //"1" for male
                }
                else {
                    gender = "2"; //"2" for female
                }
            }
        });

        userEmail = (EditText) findViewById(R.id.email_signUp);
        username = (EditText) findViewById(R.id.user_signup);
        userPW = (EditText) findViewById(R.id.pass_signup);
        userPWConfirm = (EditText) findViewById(R.id.confirmpw_signup);
        birthday = (DatePicker) findViewById(R.id.birthday_signup);
        //Get birthday info
        int day = birthday.getDayOfMonth();
        int month = birthday.getMonth() + 1;
        int year = birthday.getYear();
        //Convert birth date to string format
        final String birthdayString = String.valueOf(year)+"-"+String.valueOf(month)+"-"+String.valueOf(day);

        btnSignUp = (Button) findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check is the password matches the confirm password
//                if (userPW.getText().equals(userPWConfirm.getText())) {

                    JSONObject session = new JSONObject();

                    try {
                        session.put("email", userEmail.getText());
                        session.put("username", username.getText());
                        session.put("password", userPW.getText());
                        session.put("birthdate", birthdayString);
                        session.put("gender", gender);

                        String str_session = session.toString();
                        String result = null;

                        try {
                            result = new UserManagementThread(SignUpActivity.this).execute(getString(R.string.sign_up), str_session).get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                        JSONObject jResult = new JSONObject(result);
                        if (jResult.getBoolean("status")) {
                            //Get the user_id
                            String message = jResult.getString("message");
                            JSONObject juser_id =  new JSONObject(message);
                            String user_id = juser_id.getString("signup_ver_id");

                            //Create SharedPreferences for user_id
                            SharedPreferences.Editor userIdEditor = globalUserId.edit();
                            //put a string with key value signup_ver_id inside file global_user_id
                            userIdEditor.putString("signup_ver_id", user_id);
                            userIdEditor.commit();

                            Intent intent = new Intent(SignUpActivity.this, VerificationCodePage.class);
                            intent.putExtra("type", 1);
                            startActivity(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
//                else {
//                    Toast.makeText(SignUpActivity.this, "Password does not match confirmed password", Toast.LENGTH_SHORT);
//                }
//            }
        });
    }



}
