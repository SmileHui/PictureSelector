package com.smilehui.pictureselector.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.smilehui.pictureselector.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class PermissionCheckUtil {

    private static final String TAG = PermissionCheckUtil.class.getSimpleName();

    public PermissionCheckUtil() {
    }

    public static boolean requestPermissions(Fragment fragment, @NonNull String[] permissions) {
        return requestPermissions((Fragment) fragment, permissions, 0);
    }

    @TargetApi(23)
    public static boolean requestPermissions(Fragment fragment, @NonNull final String[] permissions, final int requestCode) {
        if (VERSION.SDK_INT < 23) {
            return true;
        } else if (permissions != null && permissions.length != 0) {
            final FragmentActivity context = fragment.getActivity();
            ArrayList permissionsNotGranted = new ArrayList();
            final int[] requestResults = new int[permissions.length];
            boolean shouldShowRequestPermissionRationale = false;
            boolean result = false;

            for (int listener = 0; listener < permissions.length; ++listener) {
                requestResults[listener] = context.checkCallingOrSelfPermission(permissions[listener]);
                if (requestResults[listener] != 0) {
                    permissionsNotGranted.add(permissions[listener]);
                    if (!shouldShowRequestPermissionRationale && !context.shouldShowRequestPermissionRationale(permissions[listener])) {
                        shouldShowRequestPermissionRationale = true;
                    }
                }
            }

            if (shouldShowRequestPermissionRationale) {
                OnClickListener var9 = new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case -2:
                                context.onRequestPermissionsResult(requestCode, permissions, requestResults);
                                break;
                            case -1:
                                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                Uri uri = Uri.fromParts("package", context.getPackageName(), (String) null);
                                intent.setData(uri);
                                context.startActivityForResult(intent, requestCode > 0 ? requestCode : -1);
                        }

                    }
                };
                showPermissionAlert(context, context.getResources().getString(R.string.rc_permission_grant_needed) + getNotGrantedPermissionMsg(context, permissionsNotGranted), var9);
            } else if (permissionsNotGranted.size() > 0) {
                context.requestPermissions((String[]) permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]), requestCode);
            } else {
                result = true;
            }
            return result;
        } else {
            return true;
        }
    }

    public static boolean requestPermissions(Activity activity, @NonNull String[] permissions) {
        return requestPermissions(activity, permissions, 0);
    }

    @TargetApi(23)
    public static boolean requestPermissions(final Activity activity, @NonNull final String[] permissions, final int requestCode) {
        if (VERSION.SDK_INT < 23) {
            return true;
        } else if (permissions != null && permissions.length != 0) {
            ArrayList permissionsNotGranted = new ArrayList();
            final int[] requests = new int[permissions.length];
            boolean shouldShowRequestPermissionRationale = false;
            boolean result = false;

            for (int listener = 0; listener < permissions.length; ++listener) {
                requests[listener] = activity.checkCallingOrSelfPermission(permissions[listener]);
                if (requests[listener] != 0) {
                    permissionsNotGranted.add(permissions[listener]);
                    if (!shouldShowRequestPermissionRationale && !activity.shouldShowRequestPermissionRationale(permissions[listener])) {
                        shouldShowRequestPermissionRationale = true;
                    }
                }
            }

            if (shouldShowRequestPermissionRationale) {
                OnClickListener var8 = new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case -2:
                                activity.onRequestPermissionsResult(requestCode, permissions, requests);
                                break;
                            case -1:
                                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                Uri uri = Uri.fromParts("package", activity.getPackageName(), (String) null);
                                intent.setData(uri);
                                activity.startActivityForResult(intent, requestCode > 0 ? requestCode : -1);
                        }

                    }
                };
                showPermissionAlert(activity, activity.getResources().getString(R.string.rc_permission_grant_needed) + getNotGrantedPermissionMsg(activity, permissionsNotGranted), var8);
            } else if (permissionsNotGranted.size() > 0) {
                activity.requestPermissions((String[]) permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]), requestCode);
            } else {
                result = true;
            }

            return result;
        } else {
            return true;
        }
    }

    public static boolean checkPermissions(Context context, @NonNull String[] permissions) {
        if (VERSION.SDK_INT < 23) {
            return true;
        } else if (permissions != null && permissions.length != 0) {
            String[] arr$ = permissions;
            int len$ = permissions.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String permission = arr$[i$];
                if (context.checkCallingOrSelfPermission(permission) != PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    private static String getNotGrantedPermissionMsg(Context context, List<String> permissions) {
        HashSet permissionsValue = new HashSet();
        Iterator result = permissions.iterator();

        while (result.hasNext()) {
            String i$ = (String) result.next();
            String permissionValue = context.getString(context.getResources().getIdentifier("rc_" + i$, "string", context.getPackageName()), new Object[]{Integer.valueOf(0)});
            permissionsValue.add(permissionValue);
        }

        String result1 = "(";

        String value;
        for (Iterator i$1 = permissionsValue.iterator(); i$1.hasNext(); result1 = result1 + value + " ") {
            value = (String) i$1.next();
        }

        result1 = result1.trim() + ")";
        return result1;
    }

    @TargetApi(11)
    private static void showPermissionAlert(Context context, String content, OnClickListener listener) {
        (new Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert)).setMessage(content)
                .setPositiveButton(R.string.rc_confirm, listener)
                .setNegativeButton(R.string.rc_cancel, listener)
                .setCancelable(false).create().show();
    }

    @TargetApi(19)
    public static boolean canDrawOverlays(Context context) {
        boolean result = true;
        if (VERSION.SDK_INT >= 23) {
            try {
                boolean booleanValue = ((Boolean) Settings.class.getDeclaredMethod("canDrawOverlays", new Class[]{Context.class}).invoke((Object) null, new Object[]{context})).booleanValue();
                Log.i(TAG, "isFloatWindowOpAllowed allowed: " + booleanValue);
                return booleanValue;
            } catch (Exception var6) {
                Log.e(TAG, String.format("getDeclaredMethod:canDrawOverlays! Error:%s, etype:%s", new Object[]{var6.getMessage(), var6.getClass().getCanonicalName()}));
                return true;
            }
        } else if (VERSION.SDK_INT < 19) {
            return true;
        } else {
            Object systemService = context.getSystemService(Context.APP_OPS_SERVICE);
            Method method;
            try {
                method = Class.forName("android.app.AppOpsManager").getMethod("checkOp", new Class[]{Integer.TYPE, Integer.TYPE, String.class});
            } catch (NoSuchMethodException var8) {
                Log.e(TAG, String.format("NoSuchMethodException method:checkOp! Error:%s", new Object[]{var8.getMessage()}));
                method = null;
            } catch (ClassNotFoundException var9) {
                var9.printStackTrace();
                method = null;
            }
            if (method != null) {
                try {
                    Integer e = (Integer) method.invoke(systemService, new Object[]{Integer.valueOf(24), Integer.valueOf(context.getApplicationInfo().uid), context.getPackageName()});
                    result = e.intValue() == 0;
                } catch (Exception var7) {
                    Log.e(TAG, String.format("call checkOp failed: %s etype:%s", new Object[]{var7.getMessage(), var7.getClass().getCanonicalName()}));
                }
            }
            Log.i(TAG, "isFloatWindowOpAllowed allowed: " + result);
            return result;
        }
    }
}