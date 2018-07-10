package com.stablekernel.twilioarchat.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.stablekernel.twilioarchat.R
import com.stablekernel.twilioarchat.config.Constants
import com.stablekernel.twilioarchat.databinding.ActivityPermissionsBinding
import com.stablekernel.twilioarchat.ui.calls.CallModel

class PermissionsActivity : AppCompatActivity() {

    private lateinit var callModel: CallModel
    private lateinit var permissionsManager: PermissionsManager
    private val viewModel = PermissionsViewModel()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        callModel = CallModel.modelFromIntent(intent)
        permissionsManager = PermissionsManager(this, callModel)
        DataBindingUtil.setContentView<ActivityPermissionsBinding>(this, R.layout.activity_permissions).apply {
            vm = viewModel
        }
    }

    override fun onResume() {
        super.onResume()
        permissionsManager.needsPermissionsFor().also {
            permissionsManager.requestPermissions(it)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != Constants.REQUEST_PERMISSIONS_CODE) return
        permissions.forEachIndexed { index, permission ->
            val isGranted = grantResults[index] == PackageManager.PERMISSION_GRANTED
            when (permission) {
                Manifest.permission.CAMERA -> { viewModel.cameraPermissionGranted = isGranted }
                Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS -> { viewModel.audioPermissionGranted = isGranted }
            }
        }
    }

    companion object {
        fun intentFor(context: Context, isHosting: Boolean, roomName: String, username: String): Intent {
            return Intent(context, PermissionsActivity::class.java)
                    .putExtra(Constants.IS_HOSTING, isHosting)
                    .putExtra(Constants.ROOM_NAME, roomName)
                    .putExtra(Constants.USERNAME, username)
        }
    }
}