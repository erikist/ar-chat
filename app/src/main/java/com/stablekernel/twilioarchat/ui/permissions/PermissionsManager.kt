package com.stablekernel.twilioarchat.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.stablekernel.twilioarchat.config.Constants.Companion.REQUEST_PERMISSIONS_CODE
import com.stablekernel.twilioarchat.ui.calls.CallModel

class PermissionsManager(private val activity: AppCompatActivity, val callModel: CallModel) {

    fun requestPermissions(permissions: List<String>) {
        permissions.forEach {
            if (isPermissionGranted(it)) {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, it)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Log.d(TAG,"shouldShowRequestPermissionRationale $it")
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(activity,
                            requiredPermissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
                    return
                    // REQUEST_PERMISSIONS_CODE is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }
    }

    fun needsPermissionsFor(): List<String> {
        return requiredPermissions.filter {
            isPermissionGranted(it)
        }
    }

    fun isPermissionGranted(permission: String): Boolean =
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "PermissionsManager"
        val requiredPermissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
    }
}