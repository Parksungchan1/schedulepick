package com.example.projectlast;

import android.app.Application;
import com.kakao.vectormap.KakaoMapSdk;

public class MyApplication extends Application {
    public static boolean kakaoMapAvailable = false;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            KakaoMapSdk.init(this, "09dec9415b32086dda0d2a5ececd931e");
            kakaoMapAvailable = true;
        } catch (Throwable t) {
            // x86_64 에뮬레이터에서는 ARM 전용 네이티브 라이브러리 로드 불가
            kakaoMapAvailable = false;
        }
    }
}
