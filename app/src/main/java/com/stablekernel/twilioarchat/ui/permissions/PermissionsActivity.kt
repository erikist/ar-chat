package com.stablekernel.twilioarchat.ui.permissions

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.stablekernel.twilioarchat.R
import com.stablekernel.twilioarchat.databinding.ActivityPermissionsBinding

class PermissionsActivity: AppCompatActivity() {

    private val viewModel = PermissionsViewModel()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        DataBindingUtil.setContentView<ActivityPermissionsBinding>(this, R.layout.activity_permissions).apply {
            vm = viewModel
        }
    }
}