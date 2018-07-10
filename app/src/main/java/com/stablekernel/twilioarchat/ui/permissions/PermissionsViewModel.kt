package com.stablekernel.twilioarchat.ui.permissions

import android.databinding.BaseObservable
import android.view.View


class PermissionsViewModel: BaseObservable() {
    var cameraPermissionGranted: Boolean? = null
        set(value) {
            field = value
            notifyChange()
        }
    var audioPermissionsGranted: Boolean? = null
        set(value) {
            field = value
            notifyChange()
        }

    val cameraProgressBarVisibility: Int
        get() = if (cameraPermissionGranted == null) View.VISIBLE else View.GONE

    val audioProgressBarVisibility: Int
        get() = if (audioPermissionsGranted == null) View.VISIBLE else View.GONE

    val cameraExVisibility: Int
        get() = if (cameraPermissionGranted == false) View.VISIBLE else View.GONE

    val audioExVisibility: Int
        get() = if (audioPermissionsGranted == false) View.VISIBLE else View.GONE

    val cameraCheckVisibility: Int
        get() = if (cameraPermissionGranted == true) View.VISIBLE else View.GONE

    val audioCheckVisibility: Int
        get() = if (audioPermissionsGranted == true) View.VISIBLE else View.GONE
}