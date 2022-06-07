package com.example.facerecog;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        录入
        Button record_button = (Button)findViewById(R.id.record_button);
        record_button.setOnClickListener(new View.OnClickListener() {
//            开始录入activity
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,RecordActivity.class);
                startActivity(intent);
            }
        });
//        打卡、考勤、宿舍
        Button daka_button = (Button)findViewById(R.id.daka_button);
        Button kaoqin_button = (Button)findViewById(R.id.kaoqin_button);
        Button sushe_button = (Button)findViewById(R.id.sushe_button);
//        打卡
        daka_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String recogType="changsuo";
                Intent intent = new Intent(MainActivity.this,RecogActivity.class);
                intent.putExtra("recogType",recogType);
                startActivity(intent);
            }
        });
//        考勤
        kaoqin_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String recogType="kaoqin";
                Intent intent = new Intent(MainActivity.this,RecogActivity.class);
                intent.putExtra("recogType",recogType);
                startActivity(intent);
            }
        });
//        宿舍
        sushe_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String recogType="sushe";
                Intent intent = new Intent(MainActivity.this,RecogActivity.class);
                intent.putExtra("recogType",recogType);
                startActivity(intent);
            }
        });
//        登录、注册
        Button register_button = (Button)findViewById(R.id.register_button);
        Button login_button = (Button)findViewById(R.id.login_button);
        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,registerActivity.class);
                startActivity(intent);
            }
        });
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,loginActivity.class);
                startActivity(intent);
            }
        });
//        历史信息
        Button history_button = (Button)findViewById(R.id.history_button);
        history_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,historyActivity.class);
                startActivity(intent);
            }
        });
    }
}