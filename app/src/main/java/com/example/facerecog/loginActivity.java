package com.example.facerecog;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class loginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editPassword, editName;
    private Button loginButton,unloginButton;

    private MyDatebseHelper dbHelper;

    private boolean isLoginSucceed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();
    }
    private void init() {
        editPassword = (EditText) findViewById(R.id.lo_password);
        editName = (EditText) findViewById(R.id.lo_username);

        loginButton = (Button) findViewById(R.id.login_btn);
        loginButton.setOnClickListener(this);

        unloginButton = (Button)findViewById(R.id.unlogin_btn);
        unloginButton.setOnClickListener(this);

        dbHelper = new MyDatebseHelper(this,"face.db",null,2);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.unlogin_btn:
                unlogin();
                break;
            case R.id.login_btn:
                login();
                break;
        }
    }
//    退出登录函数
//    未完成，清除状态有误
    public void unlogin(){
        Log.d("test","test");
//        清空S.P中保存的数据
        SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
//        注意！！！不能写入空字符串，会隐藏出错！！！
        editor.putString("username", " ");
        editor.putString("password", " ");
        editor.apply();
        isLoginSucceed =false;
        Log.d("isLogin", String.valueOf(isLoginSucceed));
//        验证是否清空
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        Log.d("t",pref.getString("username", "t"));
        Log.d("登出成功", pref.getString("username", "t") + pref.getString("password", "t"));
        Toast.makeText(getApplicationContext(), "登出成功！", Toast.LENGTH_SHORT).show();
    }

//    登录函数
    public void login() {
        final String username = editName.getText().toString().trim();
        final String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(loginActivity.this, "账号不能为空！", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(loginActivity.this, "密码不能为空！", Toast.LENGTH_SHORT).show();
        }

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final ProgressDialog pd = new ProgressDialog(loginActivity.this);
            pd.setMessage("正在登录……");
            pd.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
//                        查询数据库
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        Cursor cursor = db.query("person", null, null, null, null, null, null);
                        if (cursor.moveToFirst()) {
                            do {
                                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex("username"));
                                @SuppressLint("Range") String pwd = cursor.getString(cursor.getColumnIndex("password"));
                                if (name.equals(username) && pwd.equals(password)) {
                                    isLoginSucceed = true;
                                    Log.d("登录成功！", name);
//                                    登录成功后，账号密码写入SP
                                    SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
                                    editor.putString("username", username);
                                    editor.putString("password", password);
                                    editor.apply();
//                                    验证是否保存成功
                                    SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
                                    Log.d("usr and pwd", pref.getString("username", "") + pref.getString("password", ""));
                                    break;
                                }else {
                                    isLoginSucceed = false;
                                }
                            } while (cursor.moveToNext());
                        }
                        cursor.close();

                        Thread.sleep(2000);
                    } catch (InterruptedException e) {

                    }
                    pd.dismiss();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String messageText = isLoginSucceed ? "登录成功！" : "账号或密码错误！";
                            Toast.makeText(getApplicationContext(), messageText, Toast.LENGTH_SHORT).show();
//                            finish();
                        }
                    });
                }
            }).start();
        }
    }
}