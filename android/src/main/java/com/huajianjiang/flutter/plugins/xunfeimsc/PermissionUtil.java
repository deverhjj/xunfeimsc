
package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 权限处理工具类
 */
public class PermissionUtil {

    private PermissionUtil(){}

    /**
     * 验证请求的权限是否都已被用户授予
     *
     * @param context     权限请求所在上下文
     * @param permissions 所有需要验证的权限名称
     * @return 如果之前都被授予就返回 true，如果其中一个权限之前被拒,就返回 false
     */
    public static boolean verifyAllPermissions(@NonNull Context context, @NonNull String[] permissions)
    {
        if (permissions.length < 1) {
            return true;
        }
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(context, perm) !=
                    PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证之前权限请求后的授予情况结果
     *
     * @param grantResults 所有权限验证结果
     * @return 如果之前请求的权限都被授予了就返回 true，如果其中一个权限之前被拒,则返回 false
     */
    public static boolean verifyAllPermissionRequestResult(@NonNull int[] grantResults) {
        if (grantResults.length < 1) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 运行时权限请求
     * @param context 调用请求的对象
     * @param permissions 请求的权限
     */
    public static void requestPermissions(Object context, int requestCode, String[] permissions) {
        //检查权限请求所在的上下文环境
        if (context instanceof Activity) {
            // 权限请求所在上下文为 Framework 中的 Activity 或者 androidx 中的 Activity
            // (FragmentActivity/AppCompatActivity)
            ActivityCompat.requestPermissions((Activity) context, permissions, requestCode);
        } else if (context instanceof androidx.fragment.app.Fragment) {
            // androidx Fragment
            ((androidx.fragment.app.Fragment) context)
                    .requestPermissions(permissions, requestCode);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Framework 中的 Fragment
            ((android.app.Fragment) context).requestPermissions(permissions, requestCode);
        } else {
            throw new RuntimeException("Can not find correct context for permissions request");
        }
    }

}
