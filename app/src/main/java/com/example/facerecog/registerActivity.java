package com.example.facerecog;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class registerActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText editPassword, editName;
    private Button registerButton;

    private MyDatebseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        dbHelper = new MyDatebseHelper(this,"face.db",null,2);
        init();
    }

    private void init() {
        editPassword = (EditText) findViewById(R.id.re_password);
        editName = (EditText) findViewById(R.id.re_username);

        registerButton = (Button) findViewById(R.id.register_btn);
        registerButton.setOnClickListener(this);
    }


    @Override
        public void onClick(View v){
        Log.d("test","clicked");
        final String username = editName.getText().toString().trim();
        final String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(registerActivity.this, "账号不能为空！", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(registerActivity.this, "密码不能为空！", Toast.LENGTH_SHORT).show();
        }

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final ProgressDialog pd = new ProgressDialog(registerActivity.this);
            pd.setMessage("正在注册……");
            pd.show();

            //                        注册结果写入数据库中
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("username",username);
            values.put("password",password);
            db.insert("person",null,values);
            Log.d("message",username+password);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {

                    }
                    pd.dismiss();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            }).start();
//            Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
        }
    }
}
