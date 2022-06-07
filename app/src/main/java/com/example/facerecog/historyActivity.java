package com.example.facerecog;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class historyActivity extends AppCompatActivity {

    private MyDatebseHelper dbHelper;

    private List<historyItem> mHistoryItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initHistoryItem();
        historyAdapter adapter = new historyAdapter(historyActivity.this,R.layout.history_item,mHistoryItemList);
        ListView listView = (ListView)findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        dbHelper = new MyDatebseHelper(this,"face.db",null,2);

        Button deleteHistory = (Button)findViewById(R.id.delete_all);
        deleteHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("daka",null,null);
                Toast.makeText(getApplicationContext(),"删除成功！",Toast.LENGTH_SHORT).show();
                finish();
                Log.d("删除成功","!");
            }
        });
    }

    private void initHistoryItem(){
        dbHelper = new MyDatebseHelper(this,"face.db",null,2);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("daka",null,null,null,null,null,null);

        if (cursor.moveToFirst()){
            do {
                @SuppressLint("Range") String type = cursor.getString(cursor.getColumnIndex("type"));
                @SuppressLint("Range") String message = cursor.getString(cursor.getColumnIndex("message"));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex("name"));
                @SuppressLint("Range") String location = cursor.getString(cursor.getColumnIndex("location"));
                historyItem newItem = new historyItem(name,type,message,location);
                mHistoryItemList.add(newItem);
                Log.d("name",name+type+message+location);

            }while (cursor.moveToNext());
        }
        cursor.close();
    }
}