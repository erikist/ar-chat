package com.stablekernel.twilioarchat.ui.permissions

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.stablekernel.twilioarchat.R
import com.stablekernel.twilioarchat.config.ExtraKeys
import com.stablekernel.twilioarchat.databinding.ActivityPermissionsBinding
import com.stablekernel.twilioarchat.ui.calls.CallModel

class PermissionsActivity : AppCompatActivity() {

    private lateinit var callModel: CallModel
    private val viewModel = PermissionsViewModel()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        callModel = CallModel.modelFromIntent(intent)
        DataBindingUtil.setContentView<ActivityPermissionsBinding>(this, R.layout.activity_permissions).apply {
            vm = viewModel
        }
    }

    companion object {
        fun intentFor(context: Context, isHosting: Boolean, roomName: String, username: String): Intent {
            return Intent(context, PermissionsActivity::class.java)
                    .putExtra(ExtraKeys.IS_HOSTING, isHosting)
                    .putExtra(ExtraKeys.ROOM_NAME, roomName)
                    .putExtra(ExtraKeys.USERNAME, username)
        }
    }
}