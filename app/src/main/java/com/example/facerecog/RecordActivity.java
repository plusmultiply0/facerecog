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
//                ?????????????????????
                intent.putExtra("android.intent.extras.CAMERA_FACING",android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });

//        ??????????????????button
        Button showResult = (Button)findViewById(R.id.show_result2);
        showResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                  ??????token??????????????????????????????
                AlertDialog.Builder dialog = new AlertDialog.Builder(RecordActivity.this);
                dialog.setTitle("???????????????");
                String resultMessage;
                Log.d("token",faceToken);
                if (faceToken!=""){
                    resultMessage="???????????????";
                }else {
                    resultMessage = "???????????????????????????????????????";
                }

                dialog.setMessage(resultMessage);
                dialog.setCancelable(false);
                dialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                dialog.show();
            }
        });
//        ??????????????????button
        Button delResult = (Button)findViewById(R.id.delete_result);
        delResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ??????facetoken?????????
                if(faceToken!=""){
//                ?????????????????????????????????
                AlertDialog.Builder dialog = new AlertDialog.Builder(RecordActivity.this);
                dialog.setTitle("????????????");
                String resultMessage = "?????????????????????";
                dialog.setMessage(resultMessage);
                dialog.setCancelable(false);
                dialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
//                            ????????????-????????????
                            deleFace();

                            Log.d("facetoken",faceToken);
//                            ????????????????????????
                            AlertDialog.Builder dialog2 = new AlertDialog.Builder(RecordActivity.this);
                            dialog2.setTitle("?????????");
                            dialog2.setMessage("???????????????");
                            dialog2.setCancelable(false);
                            dialog2.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //???????????????token??????????????????
                                    faceToken="";
                                    picture.setImageBitmap(null);
                                }
                            });
                            dialog2.show();
                    }
                });
                dialog.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                dialog.show();
            }else if(faceToken==""){
                    AlertDialog.Builder dialog3 = new AlertDialog.Builder(RecordActivity.this);
                    dialog3.setTitle("?????????");
                    String resultMessage = "??????????????????????????????";
                    dialog3.setMessage(resultMessage);
                    dialog3.setCancelable(false);
                    dialog3.setPositiveButton("??????", new DialogInterface.OnClickListener() {
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
//                      ?????????????????????token
                        sendRequestForToken(newBitmap);
                        picture.setImageBitmap(newBitmap);
//                      ?????????????????????????????????????????????
                        ProgressDialog progressDialog = new ProgressDialog(RecordActivity.this);
                        progressDialog.setTitle("???????????????");
                        progressDialog.setMessage("Loading...");
                        progressDialog.setCancelable(true);
                        progressDialog.show();

//                      ??????????????????????????????
//                      ????????????????????????????????????????????????????????????????????????
                        Timer timer = new Timer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
//                                ????????????????????????
                                saveFace();
                                progressDialog.dismiss();
                                this.cancel();
//                                ????????????toast
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "????????????", Toast.LENGTH_SHORT).show();
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
    //    ??????????????????API???????????????token
    private void sendRequestForToken(Bitmap bitmap){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //???bitmap?????????byte??????
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
                    Log.d("bitmap size", String.valueOf(bitmap.getByteCount())+"height"+bitmap.getHeight()+"width"+bitmap.getWidth());
//                  ???????????????????????????????????????
                    Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,500,500,true);
                    Log.d("bitmap size", String.valueOf(newBitmap.getByteCount())+"height"+newBitmap.getHeight()+"width"+newBitmap.getWidth());

                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
//                  byte???????????????base64
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
//                    ??????json???????????????face_token
                    JSONObject obj = new JSONObject(responseData);
                    String result = obj.getString("faces");
//                  ???????????????facetoken??????
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
//    ????????????facetoken???????????????
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
//                    Toast.makeText(RecordActivity.this,"???????????????",Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
//    ????????????token
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