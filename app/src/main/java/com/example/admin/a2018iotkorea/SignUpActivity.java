package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SignUpActivity extends AppCompatActivity  {

    Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        RadioGroup radiogroup = (RadioGroup) findViewById(R.id.radiobuttonsGender);
        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton male = (RadioButton) findViewById(R.id.guy);
                RadioButton female = (RadioButton) findViewById(R.id.girl);
                if (male.isChecked()) {
                    Toast toast = Toast.makeText(SignUpActivity.this, "male checked", Toast.LENGTH_SHORT);
                    toast.show();
                    //Send "1" for male
                }
                else {
                    //Send "2" for female
                }
            }
        });

        btnSignUp = (Button) findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpActivity.this, VerificationCodePage.class);
                startActivity(intent);
            }
        });
    }



}
