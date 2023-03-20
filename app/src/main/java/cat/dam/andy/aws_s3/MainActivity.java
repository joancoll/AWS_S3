package cat.dam.andy.aws_s3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private String TAG= "AWS_S3";
    private TextView tv_data;
    private ImageView iv_s3;
    private AWS_KEYS keys= new AWS_KEYS();
    private final Executor executorService = Executors.newSingleThreadExecutor();
    private AmazonS3 s3Client;
    private S3Operations s3Operations;
    private List<String> s3Objects;
    private Spinner sp_s3_objects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_data = findViewById(R.id.tv_data);
        iv_s3 = findViewById(R.id.iv_s3);
        sp_s3_objects = findViewById(R.id.sp_s3_objects);
        sp_s3_objects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item from the Spinner
                String selectedItem = (String) parent.getItemAtPosition(position).toString();
                // Get the context
                // Do something with the selected item and context
                Toast.makeText(getApplicationContext(), "Selected item: " + selectedItem, Toast.LENGTH_SHORT).show();
                //s3Operations.downloadObject(s3Operations.getS3Client(), selectedItem);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
            transferDemoWithS3Operations();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (s3Operations != null) {
            s3Operations.shutdown();
        }
    }

    private void transferDemoWithS3Operations() {
        s3Operations = new S3Operations(this,keys.getAwsAccessKey() , keys.getAwsSecretAccessKey(), keys.getAwsSessionToken(), keys.getBucketName());
        s3Client= s3Operations.getS3Client();
        s3Objects = s3Operations.listObjects(s3Client, new S3Operations.Callback() {
            @Override
            public void onSuccess(Object result) {
                if (s3Objects.size()>0) {
                    sp_s3_objects.setVisibility(View.VISIBLE);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, s3Objects);
                    adapter.notifyDataSetChanged();
                    sp_s3_objects.setAdapter(adapter);
                    sp_s3_objects.setSelection(0); // Move to the first position in the list
                    s3Operations.deleteFile(s3Client, "demo2.mp4"); // delete the file from the bucket
                    s3Operations.uploadFile(MainActivity.this, s3Client, R.raw.demo, "video.mp4"); //upload the file to the bucket
                    File file = s3Operations.downloadFile(iv_s3, s3Client, "victory.png"); //download the file from the bucket
                    s3Operations.backupFile(s3Client, "video.mp4", "demo1.mp4"); //copy the file in the bucket
                    s3Operations.renameFile(s3Client, "demo1.mp4", "demo5.mp4"); //rename the file in the bucket
                } else {
                    sp_s3_objects.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, getString(R.string.connectionError), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, getString(R.string.connectionError), Toast.LENGTH_LONG).show();
            }
        });
    }

}