package com.stablekernel.twilioarchat.models

import com.google.gson.Gson


class ARDataMessage(val undo: Boolean? = null, val point: Point? = null) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): ARDataMessage = Gson().fromJson<ARDataMessage>(json, ARDataMessage::class.java)
    }
}

class Point(val id: Int, val x: Float, val y: Float, val isEnd: Boolean)

interface MessageListener {
    fun onMessageReceived(message: ARDataMessage)
}