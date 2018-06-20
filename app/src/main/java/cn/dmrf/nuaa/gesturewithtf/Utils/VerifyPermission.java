package cn.dmrf.nuaa.gesturewithtf.Utils;

import android.Manifest;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.github.dfqin.grantor.PermissionListener;
import com.github.dfqin.grantor.PermissionsUtil;


public class VerifyPermission {
    private Activity context;

    public VerifyPermission(Activity context) {
        this.context=context;
    }

    public void RequestPermission() {
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,Manifest.permission.WRITE_SETTINGS};
        if (PermissionsUtil.hasPermission(context, permissions)) {
            //已经获取相关权限
        } else {
            PermissionsUtil.requestPermission(context, new PermissionListener() {
                @Override
                public void permissionGranted(@NonNull String[] permission) {
                    //用户授予了权限\

                }

                @Override
                public void permissionDenied(@NonNull String[] permission) {
                    //用户拒绝了权限
                    Toast.makeText(context ,"相关权限被拒绝，本应用将无法正常运行", Toast.LENGTH_SHORT).show();
                }
            }, permissions);
        }
    }
}
