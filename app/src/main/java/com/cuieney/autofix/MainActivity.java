package com.cuieney.autofix;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cuieney.fix.DynamicApk;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

    private TextView test;
    private ImageView image;
    private Activity context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dynamic.apk";

        test = (TextView) findViewById(R.id.test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String app_name = DynamicApk.getStringFromApk(context, path, "app_name");
                Toast.makeText(context, app_name, Toast.LENGTH_SHORT).show();
                test.setTextColor(DynamicApk.getColorFromApk(context,path,"colorAccent"));
                image.setImageBitmap(BitmapFactory.decodeResource(DynamicApk.getResource(context,path),DynamicApk.getDrawableFromApk(context,path,"thumb")));

            }
        });
        image = ((ImageView) findViewById(R.id.image));

        image.setImageResource(R.drawable.thumb);

    }


}
