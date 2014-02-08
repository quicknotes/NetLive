package com.richardlucasapps.netlive;

import java.io.File;
import android.app.Application;
import android.util.Log;

public class MyApplication extends Application {
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static MyApplication getInstance() {
        return instance;
    }
}