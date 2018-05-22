package com.stablekernel.twilioarchat

import android.databinding.BaseObservable
import android.util.Log


class RoomActivityViewModel: BaseObservable() {
    var room: String = ""
        set(value) {
            field = value
            notifyChange()
        }
    var username: String = ""
        set(value) {
            field = value
            notifyChange()
        }

    val isJoinEnabled: Boolean
        get() = room.isNotBlank() && username.isNotBlank()

    fun onJoinClicked() {
        Log.d(RoomActivityViewModel::class.java.simpleName, "$username wants to join $room")
    }
}