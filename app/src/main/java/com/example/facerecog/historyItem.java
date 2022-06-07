package com.example.facerecog;

public class historyItem {
    private String name;
    private String type;
    private String message;
    private String location;
    public historyItem(String name,String type,String message,String location){
        this.message = message;
        this.type = type;
        this.name = name;
        this.location = location;
    }
    public String getName(){
        return name;
    }
    public String getType(){
        return type;
    }
    public String getMessage(){
        return message;
    }
    public String getLocation(){
        return location;
    }
}
