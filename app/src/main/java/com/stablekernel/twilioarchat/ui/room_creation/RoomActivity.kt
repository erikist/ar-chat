package com.stablekernel.twilioarchat.ui.room_creation

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.stablekernel.twilioarchat.R
import com.stablekernel.twilioarchat.databinding.ActivityRoomBinding

class RoomActivity : AppCompatActivity() {

    private val viewModel = RoomActivityViewModel(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityRoomBinding>(this, R.layout.activity_room).apply {
            vm = viewModel
        }
    }
}
