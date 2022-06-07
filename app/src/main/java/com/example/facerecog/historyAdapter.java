package com.example.facerecog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.security.acl.Group;
import java.util.List;

public class historyAdapter extends ArrayAdapter<historyItem> {
    private int resId;
    public historyAdapter(Context context, int text, List<historyItem> objects){
        super(context,text,objects);
        resId = text;
    }
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        historyItem item = getItem(position);
        @SuppressLint("ViewHolder") View view = LayoutInflater.from(getContext()).inflate(resId,parent,false);
        TextView historyItem = (TextView)view.findViewById(R.id.item);
//        根据不同的type，显示不同的信息

        String text = item.getName()+"在"+item.getLocation()+item.getMessage();
        historyItem.setText(text);
        return view;
    }
}
