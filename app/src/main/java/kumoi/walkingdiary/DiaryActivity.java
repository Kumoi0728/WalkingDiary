package kumoi.walkingdiary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class DiaryActivity extends AppCompatActivity {
    private final static String TAG = ShowActivity.class.getSimpleName();
    public Context mContext;

    private EditText diary;
    private ImageView imageView;
    private Bitmap photo = null;

    static final int TAKE_PHOTO = 1;

    public static String currentPath;
    private static File diaryFile;
    public static String photoPath;
    public static String photoName;
    public static File photoFile;
    private static Uri photoUri;

    @SuppressLint("SimpleDateFormat")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        currentPath = bundle.getString("currentPath");
        Log.i(TAG, "??????????????????=" + currentPath);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");

        /* ???????????? */
        diary = findViewById(R.id.diary);
        Button savebtn = findViewById(R.id.save);
        savebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = diary.getText().toString();

                diaryFile = new File(currentPath + "/diary_" + timeFormat.format(new Date()) + ".txt");
                Log.d(TAG, "??????????????????=" + diaryFile);
                try {
                    saveDiary(text, diaryFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        /* ????????????????????? */
        Button camerabtn = findViewById(R.id.camera);
        imageView = findViewById(R.id.image);
        camerabtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
                photoName = "photo_" + timeFormat.format(new Date()); // ?????????"/photo"??????
                try {
                    photoFile = createPhotoFile();
                    //photoUri = Uri.fromFile(photoFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (ContextCompat.checkSelfPermission(DiaryActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "???????????????????????????????????????");
                    ActivityCompat.requestPermissions(DiaryActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
                } else {
                    Log.d(TAG, "???????????????????????????????????????");
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        try {
                            startCamera();
                            Log.d(TAG, "?????????...");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //startActivityForResult(intent, TAKE_PHOTO);
                    }
                }
            }
        });
    }

    private void saveDiary(String text, File diaryFile) throws FileNotFoundException {
        Log.d(TAG, "saveDiary");
        if (diaryFile != null) {
            FileOutputStream fos = new FileOutputStream(diaryFile, true);
            try {
                fos.write(text.getBytes());
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                    Toast.makeText(DiaryActivity.this, "Memory saved in " + diaryFile, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        if (requestCode == TAKE_PHOTO) {
            //??????????????????
            if (grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "????????????????????????????????????");
                //???????????????
                try {
                    startCamera();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {  //????????????
                Log.d(TAG, "??????????????????????????????");
                Toast.makeText(this, "Need Permission!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            //photo = (Bitmap)data.getExtras().get("data");
            try {
                photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            showPhoto(photo);
            try {
                savePhoto(photo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateSystemGallery();
        }
    }

    private File createPhotoFile() throws IOException {
        Log.d(TAG, "createPhotoFile");
        File storageDir = new File(currentPath);
        File image = File.createTempFile(photoName, ".jpg", storageDir);
        photoPath = image.getAbsolutePath();
        Log.d(TAG, "??????????????????=" + photoPath);
        image.setWritable(true); //??????????????????Read-only????????????????????????SD
        return image;
    }

    private void startCamera() throws IOException {
        Log.d(TAG, "startCamera");
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //photoFile = createPhotoFile();
        //photoUri = Uri.fromFile(photoFile);
        photoUri = FileProvider.getUriForFile(this,
                this.getApplicationContext().getPackageName() + ".fileprovider",
                photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); // ???uri?????????????????????
        startActivityForResult(intent, TAKE_PHOTO); // ?????????????????????????????????
        Log.d(TAG, "?????????...");
    }

    private void showPhoto(Bitmap bitmap) {
        Log.d(TAG, "showPhoto");
        if (bitmap == null) {
            Log.d(TAG,"????????????");
            return;
        }
        imageView.setImageBitmap(bitmap);
    }

    private void savePhoto(Bitmap bitmap) throws IOException {
        Log.d(TAG, "savePhoto");
        if (photoFile != null) {
            FileOutputStream fos = new FileOutputStream(photoFile);
            /*fos.write(bytes);
            fos.flush();
            fos.close();*/
            BufferedOutputStream os = new BufferedOutputStream(fos);
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG,
                        100, //???????????????100???????????????
                        os);
            }
            try {//??????????????????????????????????????????
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(DiaryActivity.this, "Photo saved in " + photoPath, Toast.LENGTH_SHORT).show();
    }

    private void updateSystemGallery() {//??????????????????????????????
        Log.d(TAG, "updateSystemGallery");
        try {
            MediaStore.Images.Media.insertImage(this.getContentResolver(), photoPath, photoName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse(photoPath)));
    }

    /*private void savePhoto2(Intent intent, String photoName) throws FileNotFoundException {
        Log.d(TAG, "savePhoto");
        try {
            photoFile = File.createTempFile(
                    photoName,
                    ".jpg",
                    new File(currentPath)
            );
            Log.d(TAG, "??????????????????=" + photoFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (photoFile != null) {
            URI photoUri = photoFile.toURI();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            Toast.makeText(DiaryActivity.this, "Photo saved in " + photoFile, Toast.LENGTH_SHORT).show();
        }
    }*/

    protected void onResume() {
        super.onResume();
        showPhoto(photo);
    }
}

