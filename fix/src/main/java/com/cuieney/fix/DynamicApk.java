package com.cuieney.fix;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

/**
 * Created by cuieney on 05/07/2017.
 */

public class DynamicApk {
    private static int RES_STRING = 0;
    private static int RES_MIPMAP = 1;
    private static int RES_DRAWABLE = 2;
    private static int RES_LAYOUT = 3;
    private static int RES_COLOR = 4;
    private static int RES_DIMEN = 5;
    private static String TAG = "DynamicApk";

    public static void init(String apkPath) {
    }


    private static AssetManager createAssetManager(String apkPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(
                    assetManager, apkPath);
            return assetManager;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

    public static Resources getResource(Context context, String apkPath) {
        AssetManager assetManager = createAssetManager(apkPath);
        return new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
    }

    private static String getTypeFromId(int type) {
        String resType = "";
        switch (type) {
            case 0:
                resType = "string";
                break;
            case 1:
                resType = "mipmap";
                break;
            case 2:
                resType = "drawable";
                break;
            case 3:
                resType = "layout";
                break;
            case 4:
                resType = "color";
                break;
            case 5:
                resType = "dimen";
                break;
        }
        return resType;
    }

    private static int getIdFromRFile(ApplicationInfo info, DexClassLoader dexLoader, int type, String name) {
        try {
            Class<?> aClass1 = dexLoader.loadClass(info.packageName + ".R$" + getTypeFromId(type));
            Field[] declaredFields = aClass1.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.getName().equals(name)) {
                    return declaredField.getInt(R.string.class);
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "getIdFromRFile: ", e.getException());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getStringFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_STRING, name);
        return resource.getString(id);
    }

    public static Bitmap getBitmapFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_DRAWABLE, name);
        if (id == 0) {
            id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_MIPMAP, name);
        }
        return BitmapFactory.decodeResource(resource, id);
    }

    public static int getDrawableFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_DRAWABLE, name);
        return id;
    }

    public static int getMipmapFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_MIPMAP, name);
        return id;
    }

    public static int getLayoutFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_LAYOUT, name);
        return id;
    }

    public static int getColorFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_COLOR, name);

        return resource.getColor(id);
    }

    public static int getDimenFromApk(Context context, String apk, String name) {
        Resources resource = getResource(context, apk);
        int id = getIdFromRFile(getPackageInfo(context, apk), createClassLoader(apk, context), RES_DIMEN, name);
        return id;
    }

    private static DexClassLoader createClassLoader(String path, Context context) {

        DexClassLoader dexClassLoader = new DexClassLoader(path,
                context.getFilesDir().getAbsolutePath(),
                path,
                context.getClassLoader());
        return dexClassLoader;
    }

    private static ApplicationInfo getPackageInfo(Context context, String path) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        return pi.applicationInfo;
    }


}
