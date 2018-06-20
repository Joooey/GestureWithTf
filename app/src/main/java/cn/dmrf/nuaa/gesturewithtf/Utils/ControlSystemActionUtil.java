package cn.dmrf.nuaa.gesturewithtf.Utils;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by Wjyyy on 2018/6/17.
 */

public class ControlSystemActionUtil {

    private boolean hasInit = false;
    private Context context;
    private int brightness;
    private int change_value = 45;
    private AudioManager audioManager = null; // 音频

    public ControlSystemActionUtil(Context context) {
        this.context = context;

        brightness = getScreenBrightness();

    }

    //在globalbean中调用
    public void InitSys() {
        if (!hasInit) {
            ContentResolver contentResolver = context.getContentResolver();
            try {
                int mode = Settings.System.getInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE);
                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                }
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            hasInit = true;
        } else {
            return;
        }

    }

    public void lighter() {
        if (brightness <= 255 - change_value) {
            brightness += change_value;

        } else {
            brightness = 255;
        }

        setScreenBrightness(brightness);

    }

    public void darker() {
        if (brightness >= change_value) {
            brightness -= change_value;

        } else {
            brightness = 0;
        }
        setScreenBrightness(brightness);

    }

    public void showdesk() {

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        context.startActivity(i);

    }


    public void volumedown() {

        audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND
                        | AudioManager.FLAG_SHOW_UI);
    }

    public void volumeup() {

        audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND
                        | AudioManager.FLAG_SHOW_UI);
    }


    private int getScreenBrightness() {
        ContentResolver contentResolver = context.getContentResolver();
        int defVal = 125;
        return Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, defVal);
    }


    private void setScreenBrightness(int value) {
        ContentResolver contentResolver = context.getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, value);
    }


}

