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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

    private TextView test;
    private ImageView image;
    private Resources.Theme mTheme;
    private String TAG = "OYE";
    private Activity context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/app-debug-unaligned.apk";
        loadResources(path);
        
        test = (TextView) findViewById(R.id.test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(context, new Hello().say(), Toast.LENGTH_SHORT).show();
                int appName = 0;
                int image1 = 0;
//                Log.i("oye", "onClick: 1111" + mResources);
                DexClassLoader dexClassLoader = new DexClassLoader(path, MainActivity.this.getFilesDir().getAbsolutePath(), MainActivity.this.getDir("app-debug-unaligned.apk", Context.MODE_PRIVATE).getAbsolutePath(), MainActivity.this.getClassLoader());
//                Log.i(TAG, "DexClassLoader: "+dexClassLoader);
                try {
                    Class<?> aClass = dexClassLoader.loadClass("com.dodola.rocoosample" + ".HelloHack");
                    Method showHello = aClass.getMethod("showHello");
                    showHello.setAccessible(true);
                    Object o = aClass.newInstance();
                    Object invoke = showHello.invoke(o);
                    Toast.makeText(MainActivity.this, invoke +"" , Toast.LENGTH_SHORT).show();
                    Class<?> aClass1 = dexClassLoader.loadClass("com.dodola.rocoosample.R$drawable");

                    Field[] declaredFields = aClass1.getDeclaredFields();
                    for (Field declaredField : declaredFields) {
                        if (declaredField.getName().equals("app_name")) {
                            appName = declaredField.getInt(R.string.class);
                            Log.i(TAG, "appName: "+appName);
                        }
                        if (declaredField.getName().equals("thumb")) {
                            image1 = declaredField.getInt(R.drawable.class);
                            Log.i(TAG, "image1: "+image1);
                        }
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }

//                Toast.makeText(MainActivity.this, mResources.getText(2131099668), Toast.LENGTH_SHORT).show();

                image.setImageBitmap(BitmapFactory.decodeResource(mResources, image1));


            }
        });
        image = ((ImageView) findViewById(R.id.image));

        image.setImageBitmap(BitmapUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.thumb, 100, 100));

    }


    private AssetManager mAssetManager;
    private Resources mResources;

    protected void loadResources(String mDexPath) {
        Log.i("oye", "loadResources: " + new File(mDexPath).exists());
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, mDexPath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Resources superRes = super.getResources();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(),
                superRes.getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.setTo(super.getTheme());
    }
}
