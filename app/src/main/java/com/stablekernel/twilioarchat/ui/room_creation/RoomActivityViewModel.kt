package com.stablekernel.twilioarchat.ui.room_creation

import android.content.Context
import android.databinding.BaseObservable
import com.stablekernel.twilioarchat.ui.calls.CallActivity
import com.stablekernel.twilioarchat.ui.calls.CallModel

class RoomActivityViewModel(): BaseObservable() {
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

    fun onJoinClicked(context: Context) =
        context.startActivity(CallActivity.intentFor(context , CallModel.CallType.JOIN, room, username))

    fun onHostClicked(context: Context) =
        context.startActivity(CallActivity.intentFor(context , CallModel.CallType.HOST, room, username))
}