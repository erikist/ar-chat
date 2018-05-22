package com.stablekernel.twilioarchat

import android.databinding.BaseObservable


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

    val isInputValid: Boolean
        get() = room.isNotBlank() && username.isNotBlank()

    fun onJoinClicked() {

    }

    fun onHostClicked() {

    }
}