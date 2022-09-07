package kumoi.walkingdiary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TracksActivity extends AppCompatActivity {
    private final static String TAG = TracksActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        // ArrayList存放ListView中显示的项目
        String trackspath = "./data/user/0/kumoi.walkingdiary/files";
        List<String> files = readTrackFiles(trackspath);
        TextView t = (TextView) findViewById(R.id.text);
        if (files == null) {
            t.setText("No Memories...");
        } else {
            ArrayAdapter adapter = new ArrayAdapter<>(this, R.layout.view_list, R.id.text, files);
            ListView listView = (ListView) findViewById(R.id.listview);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String item = (String) parent.getItemAtPosition(position);
                    Toast.makeText(getApplicationContext(), item + " clicked", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(TracksActivity.this, ShowActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putCharSequence("path", item);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });
        }
    }

    private List<String> readTrackFiles(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            Log.e("error", "null directory");
            return null;
        }

        List<String> allfilename = new ArrayList<>();
        for (File value : files) {
            String filepath = value.getAbsolutePath();
            String filename = value.getName();
            if (isNumeric(filename)) {
                Log.d("numericfilename",filename);
            allfilename.add(filename);
            }
            //allfilename.add(filename);
        }
        return allfilename;
    }

    private boolean isNumeric(String str) {
        String[] strArray = str.split("-");
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(strArray[0]).matches();
    }
}
