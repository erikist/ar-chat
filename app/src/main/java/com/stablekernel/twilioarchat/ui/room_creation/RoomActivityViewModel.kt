package com.stablekernel.twilioarchat.ui.room_creation

import android.content.Context
import android.databinding.BaseObservable
import com.stablekernel.twilioarchat.ui.calls.CallActivity
import com.stablekernel.twilioarchat.ui.calls.CallModel

class RoomActivityViewModel(val context: Context): BaseObservable() {
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

    val isInputValid: Boolean
        get() = room.isNotBlank() && username.isNotBlank()

    fun onJoinClicked() {
        CallActivity.intentFor(context , CallModel.CallType.JOIN, room, username)
    }

    fun onHostClicked() {
        CallActivity.intentFor(context , CallModel.CallType.HOST, room, username)
    }
}