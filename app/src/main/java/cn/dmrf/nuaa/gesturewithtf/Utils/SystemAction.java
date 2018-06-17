package cn.dmrf.nuaa.gesturewithtf.Utils;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by Wjyyy on 2018/6/17.
 */

public class SystemAction {

    private Activity context;

    public SystemAction(Activity context) {
            this.context=context;
        }

    public void lighter() {
        int brightnessValue = 255 ;
        Window localWindow = context.getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        localLayoutParams.screenBrightness = brightnessValue / 255.0F;
        localWindow.setAttributes(localLayoutParams);
    }

    public void darker() {
        int brightnessValue = 50 ;
        Window localWindow = context.getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        localLayoutParams.screenBrightness = brightnessValue / 255.0F;
        localWindow.setAttributes(localLayoutParams);
    }

    public void showdesk(){

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        context.startActivity(i);
    }
    private AudioManager audioManager = null; // 音频


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

}

