package com.example.facerecog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecogActivity extends AppCompatActivity {

    public static final int TAKE_PHOTO = 1;
    private ImageView picture;
    private Uri imageUri;

    private String resConfidence ="";
    private String faceToken ;

    private MyDatebseHelper dbHelper;

    private boolean isRecogSucceed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recog);

        dbHelper = new MyDatebseHelper(this,"face.db",null,2);

//      获取传入的intent信息，将结果存入不同的数据库表项
        Intent intent = getIntent();
        String recogType = intent.getStringExtra("recogType");
        Log.d("RecogActivity",recogType);

//        设置识别button
        Button takePhoto = (Button)findViewById(R.id.take_photo2);
        picture = (ImageView)findViewById(R.id.picture2);

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File outputImage = new File(getExternalCacheDir(),"output_image.jpg");

                if (Build.VERSION.SDK_INT>=24){
                    imageUri = FileProvider.getUriForFile(RecogActivity.this,"com.example.facerecog",outputImage);
                }else{
                    imageUri=Uri.fromFile(outputImage);
                }
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
//                调用前置摄像头
                intent.putExtra("android.intent.extras.CAMERA_FACING",android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });
//        显示识别结果按钮

        Button showResult = (Button)findViewById(R.id.show_result);
        showResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                  根据confidence数据，显示不同的提示
                    AlertDialog.Builder dialog = new AlertDialog.Builder(RecogActivity.this);
                    dialog.setTitle("识别结果：");
                    String resultMessage;
                    Log.d("resconfince",resConfidence);
                    if (resConfidence!=""){
                    if(Float.parseFloat(resConfidence)>75){
                        resultMessage = "成功！";
                        isRecogSucceed =true;
                    }else {
                        resultMessage = "失败！";
                        isRecogSucceed = false;
                    }}else {
                        resultMessage = "请先识别，识别后才有结果！";
                    }

                    dialog.setMessage(resultMessage);
                    dialog.setCancelable(false);
                    dialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    dialog.show();
            }
        });

//        打卡按钮
        Button dakaButton = (Button)findViewById(R.id.daka_btn);
        dakaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                未识别人脸
                if (resConfidence.equals("")){
                    Toast.makeText(getApplicationContext(),"请先识别人脸后再打卡！",Toast.LENGTH_SHORT).show();
                }
//                人脸识别失败
                else if (!isRecogSucceed){
                    Toast.makeText(getApplicationContext(),"请先录入人脸后再打卡！",Toast.LENGTH_SHORT).show();
                }
//                人脸识别成功——才能打卡！
//                这部分代码再看看，有没有潜在的问题？？？
                else {
                    //从SP中读取数据——username
                    SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
                    Log.d("usr and pwd", pref.getString("username", "") + pref.getString("password", ""));
                    String username;

                    if (pref.getString("username","匿名用户").equals(" ")){
                         username = "匿名用户";
                    }else {
                         username = pref.getString("username","匿名用户");
                    }
//                将打卡信息写入数据库
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();

                    String location = "";
                    if (recogType.equals("changsuo")){
                        location = "场所";
                    }else if (recogType.equals("kaoqin")){
                        location = "教室";
                    }else if (recogType.equals("sushe")){
                        location = "宿舍";
                    }

                    values.put("name",username);
                    values.put("type",recogType);
                    values.put("location",location);
                    values.put("message","打卡成功！");
                    db.insert("daka",null,values);

                    Log.d("message and location",username+location);
                    Toast.makeText(getApplicationContext(),"打卡成功！",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case TAKE_PHOTO:
                if (resultCode==RESULT_OK){
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
//                      避免图片过大无法显示，先压缩图片
                        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,500,500,true);
                        picture.setImageBitmap(newBitmap);

                        //人脸图像预检测，生成face_token
                        sendRequestForToken(bitmap);

//                      识别人脸信息，开始识别前置步骤
                        ProgressDialog progressDialog = new ProgressDialog(RecogActivity.this);
                        progressDialog.setTitle("正在识别中");
                        progressDialog.setMessage("Loading...");
                        progressDialog.setCancelable(true);
                        progressDialog.show();

//                      识别完成后，取消弹窗
//                      设置定时器，一段时间后自动取消弹窗（仅暂时替代）
//                        Timer timer = new Timer();
//                        TimerTask task = new TimerTask() {
//                            @Override
//                            public void run() {
////                              使用face_token进行人脸检测
//                                sendRequestWithHttp();
//                                progressDialog.dismiss();
////                                Toast.makeText(RecogActivity.this,"识别完成！",Toast.LENGTH_SHORT).show();
//                                this.cancel();
//                            }
//                        };
//                        timer.schedule(task,3000,1000);
//                      等效代码
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(5000);
                                }catch (InterruptedException e){

                                }
                                sendRequestWithHttp();
                                progressDialog.dismiss();
//                                Toast.makeText(RecogActivity.this,"识别完成！",Toast.LENGTH_SHORT).show();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "识别完成！", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).start();

//                        progressDialog.dismiss();
                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }
//    调用人脸搜索API，检测人脸是否已存在库中
    private void sendRequestWithHttp(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    RequestBody formBody = new FormBody.Builder()
                            .add("api_key","Gq3QiB3DAWK4CK1NB7FVUIxXI0aGBb59")
                            .add("api_secret","haBPZycnBBdMfj3WFNHYjfyAIQHA6EvO")
//                            未完成，仅做示例
//                            需传入本次识别图像的生成token
                            .add("face_token",faceToken)
                            .add("faceset_token","35cf9dc51cee32cb7d197ef4061b3294").build();
                    Request request = new Request.Builder()
                            .url("https://api-cn.faceplusplus.com/facepp/v3/search").post(formBody).build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();

                    JSONObject obj = new JSONObject(responseData);
                    String result = obj.getString("results");
//                  得到嵌套的confidence数据
                    JSONArray res1 = obj.getJSONArray("results");
                    JSONObject res2 = res1.getJSONObject(0);
                    String confidence = res2.getString("confidence");
                    Log.d("result",result);
                    Log.d("confidence",confidence);
//                  保存识别分数
                    resConfidence = confidence;

//                    Log.d("res",resConfidence);
                }catch (Exception e){
                    e.printStackTrace();;
                }
            }
        }).start();
    }
//    调用人脸识别API，生成人脸token
    private void sendRequestForToken(Bitmap bitmap){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //将bitmap转换为byte数组
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
                    Log.d("bitmap size", String.valueOf(bitmap.getByteCount())+"height"+bitmap.getHeight()+"width"+bitmap.getWidth());
//                  压缩图片，避免过大无法识别
                    Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,500,500,true);
                    Log.d("bitmap size", String.valueOf(newBitmap.getByteCount())+"height"+newBitmap.getHeight()+"width"+newBitmap.getWidth());

                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
//                  byte数组编码为base64
                    String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

                    OkHttpClient client = new OkHttpClient();
                    RequestBody formBody = new FormBody.Builder()
                            .add("api_key","Gq3QiB3DAWK4CK1NB7FVUIxXI0aGBb59")
                            .add("api_secret","haBPZycnBBdMfj3WFNHYjfyAIQHA6EvO")
                            .add("image_base64",encoded).build();
                    Request request = new Request.Builder()
                            .url("https://api-cn.faceplusplus.com/facepp/v3/detect").post(formBody).build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Log.d("response",responseData);
//                    解析json数据，提取face_token
                    JSONObject obj = new JSONObject(responseData);
                    String result = obj.getString("faces");
//                  得到嵌套的confidence数据
                    JSONArray res1 = obj.getJSONArray("faces");
                    JSONObject res2 = res1.getJSONObject(0);
                    faceToken = res2.getString("face_token");
                    Log.d("result1",result);
                    Log.d("facetoken",faceToken);
                }catch (Exception e){
                    e.printStackTrace();;
                }
            }
        }).start();
    }
}