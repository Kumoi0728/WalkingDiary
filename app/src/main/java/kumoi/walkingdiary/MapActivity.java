package kumoi.walkingdiary;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import static kumoi.walkingdiary.MyApplication.getContext;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


@RequiresApi(api = Build.VERSION_CODES.Q)
public class MapActivity extends AppCompatActivity
        implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback, // OnMapReadyCallback 用于处理 GoogleMap 对象的事件和用户互动的回调接口
        ActivityCompat.OnRequestPermissionsResultCallback {

    private final static String TAG = MapActivity.class.getSimpleName();
    private static Context mContext;

    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    private final static int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private LocationManager locationManager;
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            try {
                locationUpdates(location, trackFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    };

    private String filename;
    private GoogleMap mMap;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    public File filesDir;
    public static String currentPath;
    public static File trackFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        filesDir = getFilesDir(); // /data/user/0/kumoi.walkingdiary/files
        Log.i(TAG, "存储路径=" + filesDir);

        /* 显示地图，SupportMapFragment: 用于管理 GoogleMap 对象生命周期的 fragment
           若要在应用中使用 GoogleMap 对象，必须先将 SupportMapFragment 或 MapView 对象用作地图的容器对象，
           然后再从该容器中检索 GoogleMap 对象。*/
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            Log.d(TAG, "getMapAsync");
            mapFragment.getMapAsync(this);
        }

        /* WALK 按钮事件 */
        Button walkbtn = findViewById(R.id.start_walking);
        final boolean[] isWalking = {false};
        walkbtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
            @Override
            public void onClick(View view) {
                if (!isWalking[0]) { // 开始记录轨迹
                    walkbtn.setText("STOP");
                    Log.d(TAG, "start recording location");
                    Toast.makeText(MapActivity.this, "Start Recording Location", Toast.LENGTH_SHORT).show();

                    SimpleDateFormat dirFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    currentPath = mk_dateFile(dirFormat.format(new Date()));
                    trackFile = new File(currentPath + "/track.txt");
                    Log.d(TAG, "轨迹文件路径=" + trackFile);
                    try {
                        Location location = recordLocation();
                        locationUpdates(location, trackFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    isWalking[0] = true;
                } else { // 停止
                    walkbtn.setText("START");
                    Log.d(TAG, "end recording location");
                    Toast.makeText(MapActivity.this, "End Recording Location", Toast.LENGTH_SHORT).show();
                    try {
                        stopLocationUpdate();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    isWalking[0] = false;
                    Toast.makeText(MapActivity.this, "Walking saved in " + trackFile, Toast.LENGTH_SHORT).show();
                }
            }
        });

        /* WRITE DAIRY按钮事件：跳转添加图文日记 */
        Button diarybtn = findViewById(R.id.write_diary);
        diarybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "write dairy");
                Intent intent = new Intent(MapActivity.this, DiaryActivity.class);
                Bundle bundle = new Bundle();
                bundle.putCharSequence("currentPath", currentPath);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
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
        Location location = recordLocation();
        //locationUpdates(location, filename);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        try {
            stopLocationUpdate();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    /* 使用 onMapReady 回调方法获取 GoogleMap 对象，将在地图准备好接收用户输入时触发。
       它会提供 GoogleMap 类的非空实例，用来更新地图。*/
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.d(TAG, "onMapReady");
        this.mMap = map;

        UiSettings mUiSettings = map.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.6051, 139.6835), 20));
        // 权限检查
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        // 在地图上启用“我的位置”图层
        map.setMyLocationEnabled(true);
        // 如果点击“我的位置”按钮，会收到来自 OnMyLocationButtonClickListener 的 onMyLocationButtonClick() 回调。
        map.setOnMyLocationButtonClickListener(this);
        // 如果点击“我的位置”蓝点，会收到来自 OnMyLocationClickListener 的 onMyLocationClick() 回调
        map.setOnMyLocationClickListener(this);

        // 向地图添加标记
        // map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Log.d(TAG, "MyLocation button clicked");
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 20));
                        }
                    }
                });
        return true;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_SHORT).show();
    }

    private Location recordLocation() {
        Log.d(TAG, "recordLocation");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
        }

        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER,
                5000,
                1,
                mLocationListener);
        return locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER);
    }

    private void locationUpdates(Location location, File trackFile) throws FileNotFoundException {
        Log.d(TAG, "locationUpdates");
        if (trackFile != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            FileOutputStream fos = new FileOutputStream(trackFile, true);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
            String text = timeFormat.format(new Date()) + " " + lat + "," + lng + "\n";
            try {
                fos.write(text.getBytes());
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        /*float lat = (float) location.getLatitude();
        float lng = (float) location.getLongitude();

        if (location != null) {
            mContext = getContext();
            sp = mContext.getSharedPreferences(filename, MODE_PRIVATE);
            //sp =getDefaultSharedPreferences(this);
            Log.d(TAG, "上下文" + mContext);
            Log.d(TAG, "记录sp" + sp);
            editor = sp.edit();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss");
            String keyname = dateFormat.format(new Date());
            editor.putFloat("lat_" + keyname, lat);
            editor.putFloat("lng_" + keyname, lng);
            //editor.commit();
        } else {
            Toast.makeText(this, "Null location information!", Toast.LENGTH_SHORT).show();
        }*/
        }
    }

    private void stopLocationUpdate() throws FileNotFoundException {
        Log.d(TAG, "stopLocationUpdate");
        locationManager.removeUpdates(mLocationListener);
        FileOutputStream fos = new FileOutputStream(trackFile, true);
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*@Override
    public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions, @NonNull int[] grants) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (reqCode == LOCATION_PERMISSION_REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow in your app.
                //startLocationUpdate(false);
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                super.onRequestPermissionsResult(reqCode, permissions, grants);
            }
        }
    }*/

    private String mk_dateFile(String date) {
        String savePath = filesDir.toString() + "/" + date;
        File dateDir = new File(savePath);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
            Log.d("创建存储路径=", savePath);
        }
        return savePath;
    }
}
