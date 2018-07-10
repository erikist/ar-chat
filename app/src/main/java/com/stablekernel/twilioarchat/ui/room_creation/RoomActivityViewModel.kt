package com.stablekernel.twilioarchat.ui.room_creation

import android.content.Context
import android.databinding.BaseObservable
import com.stablekernel.twilioarchat.ui.permissions.PermissionsActivity

class RoomActivityViewModel : BaseObservable() {
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
        context.startActivity(PermissionsActivity.intentFor(context , false, room, username))

    fun onHostClicked(context: Context) =
        context.startActivity(PermissionsActivity.intentFor(context , true, room, username))
}