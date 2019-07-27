package com.dave.wifirealtimespeeker.util;

import android.widget.Toast;

import win.lioil.bluetooth.APP;


/**
 * Created by Kevin on 2016/10/24.
 */
public class CommUtil {

    public final static String tag = "【CommUtil】";

    public static void Toast(String content) {
        Toast.makeText(APP.getContext(), content, Toast.LENGTH_SHORT).show();
    }
}
