package com.example.facerecog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecordActivity extends AppCompatActivity {

    public static final int TAKE_PHOTO = 1;
    private ImageView picture;
    private Uri imageUri;

    private String faceToken ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        Button takePhoto = (Button)findViewById(R.id.take_photo);
        picture = (ImageView)findViewById(R.id.picture);

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File outputImage = new File(getExternalCacheDir(),"output_image.jpg");

                if (Build.VERSION.SDK_INT>=24){
                    imageUri = FileProvider.getUriForFile(RecordActivity.this,"com.example.facerecog",outputImage);
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

//        设置查询结果button
        Button showResult = (Button)findViewById(R.id.show_result2);
        showResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                  根据token数据，显示不同的提示
                AlertDialog.Builder dialog = new AlertDialog.Builder(RecordActivity.this);
                dialog.setTitle("识别结果：");
                String resultMessage;
                Log.d("token",faceToken);
                if (faceToken!=""){
                    resultMessage="录入成功！";
                }else {
                    resultMessage = "请先录入，录入后才有结果！";
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
//        删除录入结果button
        Button delResult = (Button)findViewById(R.id.delete_result);
        delResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                确认facetoken已生成
                if(faceToken!=""){
//                弹出对话框确认是否删除
                AlertDialog.Builder dialog = new AlertDialog.Builder(RecordActivity.this);
                dialog.setTitle("删除确认");
                String resultMessage = "是否确认删除？";
                dialog.setMessage(resultMessage);
                dialog.setCancelable(false);
                dialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
//                            删除人脸-网络请求
                            deleFace();

                            Log.d("facetoken",faceToken);
//                            删除完成后的提示
                            AlertDialog.Builder dialog2 = new AlertDialog.Builder(RecordActivity.this);
                            dialog2.setTitle("提示：");
                            dialog2.setMessage("删除成功！");
                            dialog2.setCancelable(false);
                            dialog2.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //清空保存的token和设置的图像
                                    faceToken="";
                                    picture.setImageBitmap(null);
                                }
                            });
                            dialog2.show();
                    }
                });
                dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                dialog.show();
            }else if(faceToken==""){
                    AlertDialog.Builder dialog3 = new AlertDialog.Builder(RecordActivity.this);
                    dialog3.setTitle("提示：");
                    String resultMessage = "请先录入，才能删除！";
                    dialog3.setMessage(resultMessage);
                    dialog3.setCancelable(false);
                    dialog3.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    dialog3.show();
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

                        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,500,500,true);
//                      发送人脸，得到token
                        sendRequestForToken(newBitmap);
                        picture.setImageBitmap(newBitmap);
//                      录入人脸信息，开始识别前置步骤
                        ProgressDialog progressDialog = new ProgressDialog(RecordActivity.this);
                        progressDialog.setTitle("正在录入中");
                        progressDialog.setMessage("Loading...");
                        progressDialog.setCancelable(true);
                        progressDialog.show();

//                      识别完成后，取消弹窗
//                      设置定时器，一段时间后自动取消弹窗（仅暂时替代）
                        Timer timer = new Timer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
//                                保存人脸数据入库
                                saveFace();
                                progressDialog.dismiss();
                                this.cancel();
//                                显示结果toast
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "录入成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        };
                        timer.schedule(task,3000,10000);

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
//                  得到嵌套的facetoken数据
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
//    将得到的facetoken录入人脸库
    private void saveFace(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    RequestBody formBody = new FormBody.Builder()
                            .add("api_key","Gq3QiB3DAWK4CK1NB7FVUIxXI0aGBb59")
                            .add("api_secret","haBPZycnBBdMfj3WFNHYjfyAIQHA6EvO")
                            .add("faceset_token","35cf9dc51cee32cb7d197ef4061b3294")
                            .add("face_tokens",faceToken).build();
                    Request request = new Request.Builder()
                            .url("https://api-cn.faceplusplus.com/facepp/v3/faceset/addface").post(formBody).build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Log.d("response",responseData);
//                    Toast.makeText(RecordActivity.this,"识别完成！",Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
//    删除人脸token
    private void deleFace(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    RequestBody formBody = new FormBody.Builder()
                            .add("api_key", "Gq3QiB3DAWK4CK1NB7FVUIxXI0aGBb59")
                            .add("api_secret", "haBPZycnBBdMfj3WFNHYjfyAIQHA6EvO")
                            .add("faceset_token", "35cf9dc51cee32cb7d197ef4061b3294")
                            .add("face_tokens", faceToken).build();
                    Request request = new Request.Builder()
                            .url("https://api-cn.faceplusplus.com/facepp/v3/faceset/removeface").post(formBody).build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    Log.d("response1", responseData);
                }catch (Exception e){
                    e.printStackTrace();;
                }
            }
        }).start();
    }
}