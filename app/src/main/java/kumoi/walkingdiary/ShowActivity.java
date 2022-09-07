package kumoi.walkingdiary;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;

public class ShowActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final static String TAG = ShowActivity.class.getSimpleName();

    private String path;
    private String trackFile;
    private String diaryFile;
    private String photoFile;


    private GoogleMap mMap;
    private LatLng photoLatLng;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            Log.d(TAG, "getMapAsync");
            mapFragment.getMapAsync(this);
        }

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        path = "./data/user/0/kumoi.walkingdiary/files/" + bundle.getString("path");
        Log.d(TAG, "传入文件夹" + path);
        getFiles();

        /*mContext = getContext();
        //sp = mContext.getSharedPreferences(xmlfile, ShowActivity.MODE_PRIVATE);
        //File tracks2 = new File(file);
        sp = getDefaultSharedPreferences(this);

        Log.d(TAG, "上下文" + getApplicationContext());
        Log.d(TAG, "显示sp" + sp);

        Map<String, ?> tracks = sp.getAll();
        Log.d(TAG, "sp 文件大小 = " + tracks.size());
        Map<String, Float> lat = new HashMap<>();
        Map<String, Float> lng = new HashMap<>();
        times = new ArrayList<>();
        for (String key : tracks.keySet()) {
            float value = (float) tracks.get(key);
            String[] array = key.split("_");
            times.add(array[1]);
            if (Objects.equals(array[0], "lat")) {
                lat.put(array[1], value);
            } else {
                lng.put(array[1], value);
            }
        }

        times = removeRepeat(times);
        times.sort(Comparator.naturalOrder());
        //Log.d(TAG, "t 文件大小 = " + t.size());
        latLng = new ArrayList<>();
        polylineOptions = new PolylineOptions().color(Color.GREEN);
        for (int i = 0; i < times.size(); i++) {
            String time = (String) times.get(i);
            //Log.d(TAG, "时间 = " + time);
            LatLng ll = new LatLng(lat.get(time), lng.get(time));
            //Log.d(TAG, "ll经纬度 = " + ll);
            latLng.add(ll);
            polylineOptions.add(ll);
        }
        //polylineOptions = new PolylineOptions().addAll(latLng);*/

        /* 处理日记文件 */
        TextView diary = findViewById(R.id.diary);
        String text = null;
        if (diaryFile != null) {
            try {
                text = readDiary();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        diary.setText(Objects.requireNonNullElse(text,
                "You write nothing in this walking..."));

        /* 处理照片文件 */
        ImageView photo = findViewById(R.id.photo);
        if (photoFile != null) {
            photoLatLng = getExif(photoFile);
            photo.setImageURI(Uri.fromFile(new File(photoFile)));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.d(TAG, "onMapReady");
        this.mMap = map;

        UiSettings mUiSettings = map.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);

        try {
            drawTrack(map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFiles() {
        Log.d(TAG, "getFiles");
        if (path != null) {
            File file = new File(path);
            File[] files = file.listFiles();
            assert files != null;
            for (File value : files) {
                String filename = value.getName();
                if (filename.charAt(0) == 't') {
                    trackFile = value.toString();
                    Log.d(TAG, "轨迹文件" + trackFile);
                }
                if (filename.charAt(0) == 'd') {
                    diaryFile = value.toString();
                    Log.d(TAG, "日记文件" + diaryFile);
                }
                if (filename.charAt(0) == 'p') {
                    photoFile = value.toString();
                    Log.d(TAG, "照片文件" + photoFile);
                }
            }
        }
    }

    private String readTracks() throws IOException {
        Log.d(TAG, "readTracks()读取轨迹" + trackFile);
        FileInputStream fis = new FileInputStream(trackFile);
        StringBuilder result = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader buffreader = new BufferedReader(isr);
        String line;
        while ((line = buffreader.readLine()) != null) { // 分行读取
            result.append(line).append("\n");
        }
        fis.close();
        return result.toString();
    }

    private String readDiary() throws IOException {
        Log.d(TAG, "readDiary()读取日记" + diaryFile);
        FileInputStream fis = new FileInputStream(diaryFile);
        int length = diaryFile.length();
        byte[] buff = new byte[length];
        fis.read(buff);
        fis.close();
        return new String(buff);
    }

    private void drawTrack(GoogleMap map) throws IOException {
        Log.d(TAG, "drawTrack");
        String[] lines = readTracks().split("\n");
        ArrayList<LatLng> latLng = new ArrayList<>();
        PolylineOptions polylineOptions = new PolylineOptions();
        for (String line : lines) {
            String time = line.split(" ")[0];
            double lat = Double.parseDouble(line.split(" ")[1].split(",")[0]);
            double lng = Double.parseDouble(line.split(" ")[1].split(",")[1]);
            latLng.add(new LatLng(lat, lng));
            polylineOptions.add(new LatLng(lat, lng)).color(Color.GREEN);
        }
        //polylineOptions.addAll(latLng).color(Color.GREEN);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng.get(latLng.size() - 1),
                20));
        Polyline polyline = mMap.addPolyline(polylineOptions);
        map.addMarker(new MarkerOptions().position(latLng.get(0)).title("Start"));
        map.addMarker(new MarkerOptions().position(latLng.get(latLng.size() - 1)).title("End"));
        if (photoLatLng != null) {
            map.addMarker(new MarkerOptions().position(photoLatLng).title("Photo"));
        } else {
            map.addMarker(new MarkerOptions().position(latLng.get(0)).title("No photo"));
        }
    }
    /*public static ArrayList removeRepeat(ArrayList arr) {
        Set set = new HashSet();
        for (int i = 0; i < arr.size(); i++) {
            set.add(arr.get(i));
        }
        return new ArrayList<>(set);
    }*/

    private LatLng getExif(String filename) {
        Log.d(TAG, "getExif");
        InputStream stream = null;
        try {
            //stream = getContentResolver().openInputStream(Uri.parse(filename));
            if (filename != null) {//stream != null) {
                //ExifInterface exifInterface = null;
                //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                //exifInterface = new ExifInterface(stream);
                //}
                ExifInterface exifInterface = new ExifInterface(filename);
                //assert exifInterface != null;
                float[] latLong = new float[2];
                boolean hasLatLong = exifInterface.getLatLong(latLong);
                if (hasLatLong) {
                    LatLng ll = new LatLng(latLong[0], latLong[1]);
                    Log.d(TAG, "照片经纬度" + ll);
                    return ll;
                }
                else{
                    Log.d(TAG, "照片经纬度=null");
                }
                //stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
