package com.example.anlan.qrtracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class InfoActivity extends Activity {
    private EditText name;
    private Spinner age;
    private Spinner gender;
    private Spinner tech_level;
    private Spinner arexp;
    private Button confirm;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        // name, gender, tech_level, age, arexp
        final String[] informations = new String[5];

        name = (EditText) findViewById(R.id.name);
        age = (Spinner) findViewById(R.id.age_select);
        gender = (Spinner) findViewById(R.id.gender);
        tech_level = (Spinner) findViewById(R.id.tech_level_select);
        arexp = (Spinner) findViewById(R.id.arexp_select);
        confirm = (Button) findViewById(R.id.confirm);

        gender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] gendervalues = getResources().getStringArray(R.array.gender);
                informations[1] = gendervalues[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] gendervalues = getResources().getStringArray(R.array.gender);
                informations[1] = gendervalues[0];
            }
        });

        tech_level.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] techlevelvalues = getResources().getStringArray(R.array.tech_level);
                informations[2] = techlevelvalues[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] techlevelvalues = getResources().getStringArray(R.array.tech_level);
                informations[2] = techlevelvalues[0];
            }
        });

        age.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] agevalues = getResources().getStringArray(R.array.age);
                informations[3] = agevalues[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] agevalues = getResources().getStringArray(R.array.age);
                informations[3] = agevalues[0];
            }
        });

        arexp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] arexpvalues = getResources().getStringArray(R.array.yorn);
                informations[4] = arexpvalues[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] arexpvalues = getResources().getStringArray(R.array.yorn);
                informations[4] = arexpvalues[0];
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                informations[0] = name.getText().toString();
                Intent data = new Intent();
                data.putExtra("userinfo", informations);
                setResult(0, data);
                finish();
            }
        });
    }
}
