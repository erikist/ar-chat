package com.stablekernel.twilioarchat.ui.permissions

import android.databinding.BaseObservable
import android.view.View


class PermissionsViewModel: BaseObservable() {
    var cameraPermissionGranted: Boolean? = null
    var audioPermissionsGranted: Boolean? = null

    val cameraProgressBarVisibility: Int
        get() = if (cameraPermissionGranted == null) View.VISIBLE else View.GONE

    val audioProgressBarVisibility: Int
        get() = if (cameraPermissionGranted == null) View.VISIBLE else View.GONE

}