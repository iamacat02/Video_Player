package i.am.a.cat.streemify.ui.screen;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import i.am.a.cat.streemify.R;
import i.am.a.cat.streemify.ui.MainActivity;

public class SplashActivity extends AppCompatActivity {
    private Boolean isReadyToExit = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
            splashScreen.setKeepOnScreenCondition(() -> !isReadyToExit);

        } else {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar);
            setContentView(R.layout.activity_splash_screen);
        }

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {

            ImageView appImage = findViewById(R.id.revealImage);
            TextView appName = findViewById(R.id.appName);

            Animation animation = AnimationUtils.loadAnimation(this, R.anim.alpha_reveal);

            appImage.setAnimation(animation);
            appName.setAnimation(animation);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isReadyToExit = true;
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 1800);
    }
}