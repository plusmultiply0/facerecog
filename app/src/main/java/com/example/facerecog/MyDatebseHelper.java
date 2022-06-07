package com.example.facerecog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class MyDatebseHelper extends SQLiteOpenHelper {
    public static final String CREATE_PERSON = "create table person ("
            +"id integer primary key autoincrement,"
            +"username text,"
            +"password text)";

    public static final String CREATE_DAKA = "create table daka("
            +"id integer primary key autoincrement,"
            +"type text,"
            +"message text,"
            +"location text,"
            +"name text)";

    private Context mContext;

    public MyDatebseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
        mContext = context;
    }
    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(CREATE_PERSON);
        db.execSQL(CREATE_DAKA);
        Toast.makeText(mContext,"create succeeded",Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        db.execSQL("drop table if exists person");
        db.execSQL("drop table if exists daka");
        onCreate(db);
    }
}
