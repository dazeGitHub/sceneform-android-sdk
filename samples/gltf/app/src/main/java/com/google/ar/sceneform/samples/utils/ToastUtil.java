package com.google.ar.sceneform.samples.utils;

import com.hjq.toast.ToastUtils;

public class ToastUtil {

    private ToastUtil() { }

    public static void showShortToast(CharSequence text) {
        ToastUtils.show(text);
    }
}
