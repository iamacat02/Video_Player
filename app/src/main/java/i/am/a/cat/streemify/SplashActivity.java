package i.am.a.cat.streemify;

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

    public class SplashActivity extends AppCompatActivity {
        private Boolean isReadyToExit = false;
        private SplashScreen splashScreen;
        private ImageView appImage;
        private TextView appName;
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ এর জন্য নতুন Splash Screen API ব্যবহার
                splashScreen = SplashScreen.installSplashScreen(this);
                splashScreen.setKeepOnScreenCondition(new SplashScreen.KeepOnScreenCondition() {
                    @Override
                    public boolean shouldKeepOnScreen() {
                        return !isReadyToExit;
                    }
                });
            } else {
                // Android 6-11 এর জন্য পুরনো মেথড ব্যবহার
                setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar);
                setContentView(R.layout.activity_splash_screen);
            }
            super.onCreate(savedInstanceState);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                appImage = findViewById(R.id.revealImage);
                appName = findViewById(R.id.appName);

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