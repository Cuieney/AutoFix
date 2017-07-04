package com.cuieney.autofix;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.cuieney.fix.AutoFix;

/**
 * Created by cuieney on 29/06/2017.
 */

public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AutoFix.init(this);
        AutoFix.applyPatch(this,Environment.getExternalStorageDirectory().getAbsolutePath()+"/patch.jar");
    }
}
