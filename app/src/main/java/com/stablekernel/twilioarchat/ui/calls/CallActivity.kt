package com.stablekernel.twilioarchat.ui.calls

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.stablekernel.twilioarchat.BuildConfig
import com.stablekernel.twilioarchat.R
import com.stablekernel.twilioarchat.config.ExtraKeys
import com.stablekernel.twilioarchat.models.ARDataMessage
import com.twilio.video.*
import java.nio.ByteBuffer


class CallActivity: AppCompatActivity(), CallCapable {

    private lateinit var callModel: CallModel
    private lateinit var callManager: CallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temporary)
        callModel = modelFromIntent(intent).apply {
            callCapabilities = this@CallActivity
        }
    }

    override fun onResume() {
        super.onResume()
        if (callModel.isHost && callModel.canMakeCall) {
            callManager.makeCall(callModel)
        }
    }

    companion object {
        fun intentFor(context: Context, type: CallModel.CallType, roomName: String, username: String): Intent {
            return Intent(context, CallActivity::class.java)
                    .putExtra(ExtraKeys.IS_HOSTING, type == CallModel.CallType.HOST)
                    .putExtra(ExtraKeys.ROOM_NAME, roomName)
                    .putExtra(ExtraKeys.USERNAME, username)
        }

        fun modelFromIntent(intent: Intent): CallModel {
            return CallModel(
                    if(intent.extras.getBoolean(ExtraKeys.IS_HOSTING)) CallModel.CallType.HOST else CallModel.CallType.JOIN,
                    intent.extras.getString(ExtraKeys.ROOM_NAME),
                    intent.extras.getString(ExtraKeys.USERNAME)
            )
        }
    }
}

class CallManager(val context: Context, val listener: AVListener) : RemoteDataTrack.Listener {

    var room: Room? = null

    var videoManager: VideoManager? = null
        set(value) {
            field = value
            //roomListener = value?.let { RoomListener(it) }
        }
    private var roomListener: RoomListener? = null

    private var localDataTrack: LocalDataTrack? = null
    var localParticipant: LocalParticipant? = null
    private var participantListener = videoManager?.let { ParticipantListener(it, this) }
    private var participantIdentity: String? = null

    fun makeCall(callModel: CallModel) {
        val connectOptionsBuilder = ConnectOptions.Builder(BuildConfig.TWILIO_API_KEY)
                .roomName(callModel.roomName)

        videoManager?.localVideoTrack?.let {
            connectOptionsBuilder.videoTracks(listOf(it))
            videoManager!!.bindLocalVideo()
        }

        localDataTrack = LocalDataTrack.create(context)
        connectOptionsBuilder.dataTracks(listOf(localDataTrack))

        room = roomListener?.let { Video.connect(context, connectOptionsBuilder.build(), it) }
    }

    fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        participantIdentity = remoteParticipant.identity

        /*
         * Add participant renderer
         */
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let { videoManager?.addRemoteParticipantVideo(it) }
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(participantListener)
        remoteParticipant.remoteDataTracks.firstOrNull()?.remoteDataTrack?.setListener(this)
        listener.remoteParticipantConnected()
    }

    /*
     * Called when participant leaves the room
    */
    fun removeRemoteParticipant(remoteParticipant: RemoteParticipant) {
        if (remoteParticipant.identity != participantIdentity) {
            return
        }

        /*
         * Remove participant renderer
         */
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let { videoManager?.removeParticipantVideo(it) }
            }
        }

        listener.remoteParticipantDisconnected()
    }

    fun onDisconnected() {
        localParticipant = null
        room = null
    }

    fun publish(videoTrack: LocalVideoTrack) {
        localParticipant?.publishTrack(videoTrack)
    }

    fun onFailure() {
        listener.onFailure()
    }

    fun send(arDataMessage: ARDataMessage) {
        Log.d(TAG, arDataMessage.toJson())
        localDataTrack?.send(arDataMessage.toJson())
    }

    override fun onMessage(remoteDataTrack: RemoteDataTrack?, messageBuffer: ByteBuffer?) {
        //NOP
    }

    override fun onMessage(remoteDataTrack: RemoteDataTrack?, message: String?) {
        Log.d(TAG, message)
//        message?.let { messageListener?.onMessageReceived(ARDataMessage.fromJson(message)) }
    }

    companion object {
        const val TAG = "RoomManager"
    }
}

class RoomListener(val callManager: CallManager) : Room.Listener {
    companion object {
        const val TAG = "RoomListener"
    }

    override fun onConnected(room: Room) {
        callManager.localParticipant = room.localParticipant

        // Only one participant is supported
        room.remoteParticipants.firstOrNull()?.let { callManager.addRemoteParticipant(it) }
    }

    override fun onConnectFailure(room: Room, e: TwilioException) {
        callManager.onFailure()
    }

    override fun onDisconnected(room: Room, e: TwilioException?) {
        callManager.onDisconnected()
    }

    override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
        callManager.addRemoteParticipant(participant)
    }

    override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
        callManager.removeRemoteParticipant(participant)
    }

    override fun onRecordingStarted(room: Room) {
        Log.d(TAG, "onRecordingStarted")
    }

    override fun onRecordingStopped(room: Room) {
        Log.d(TAG, "onRecordingStopped")
    }
}

class VideoManager(private val callManager: CallManager, private var videoRenderer: VideoRenderer? = null, private var videoCapturer: VideoCapturer? = null) {
    internal var localVideoTrack: LocalVideoTrack? = null

    fun addRemoteParticipantVideo(videoTrack: RemoteVideoTrack) {
        videoRenderer?.let { videoTrack.addRenderer(it) }
    }

    fun removeParticipantVideo(videoTrack: RemoteVideoTrack) {
        //TODO this should affect timers and whatnot. Also consider making call versus receiving a call
        videoRenderer?.let { videoTrack.removeRenderer(it) }
    }

    fun onResume() {
        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        localVideoTrack = if (localVideoTrack == null) {
            videoCapturer?.let {
                LocalVideoTrack.create(callManager.context,
                        true,
                        it)
            }
        } else {
            localVideoTrack
        }
        videoRenderer?.let { localVideoTrack?.addRenderer(it) }

        /*
         * If connected to a Room then share the local video track.
         */
        localVideoTrack?.let { callManager.publish(it) }

        /*
         * If we start to care about updating encoding parameters -- this would be the place to do it
         *
         * localParticipant?.setEncodingParameters(encodingParameters)
         */
    }

    fun release() {
        /*
        * Release the local audio and video tracks ensuring any memory allocated to audio
        * or video is freed.
        */
        localVideoTrack?.release()
    }

    fun createVideoTrack() {
        localVideoTrack = videoCapturer?.let { LocalVideoTrack.create(callManager.context, true, it) }
        localVideoTrack?.let { callManager.publish(it) }
    }

    fun onPause() {
        localVideoTrack?.let { callManager.localParticipant?.unpublishTrack(it) }


        /*
         * Release the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */
        localVideoTrack?.release()
        localVideoTrack = null
    }

    fun bindLocalVideo() {
        videoRenderer?.let { localVideoTrack?.addRenderer(it) }
    }
}

class CallModel(val type: CallType, val roomName: String, val username: String) {
    enum class CallType {
        HOST, JOIN
    }

    var callCapabilities: CallCapable? = null

    val canMakeCall: Boolean
        get() {
            return when(type) {
                CallModel.CallType.HOST -> callCapabilities?.canUseAr() == true
                CallModel.CallType.JOIN -> true
            }
        }
    val isHost: Boolean
        get() = type == CallType.HOST
}

interface CallCapable {
    fun canUseAr():Boolean = false
    fun isArCoreInstalled():Boolean = false
    fun requestArCore() {
        if (isArCoreInstalled()) {
            throw IllegalStateException("Cannot Request ARCore download when ARCore is already installed")
        }
        if (!canUseAr()) {
            throw IllegalStateException("Cannot Request ARCore when the device is incapable of using ARCore")
        }
        throw IllegalStateException("Accessing requestArCore() should only be done from MakingCallActivity")
    }
}

class ParticipantListener(private val videoManager: VideoManager, private val dataListener: RemoteDataTrack.Listener) : RemoteParticipant.Listener {

    companion object {
        val TAG = "ParticipantListener"
    }

    override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant,
                                       remoteAudioTrackPublication: RemoteAudioTrackPublication) {
        Log.i(TAG, "onAudioTrackPublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                "name=${remoteAudioTrackPublication.trackName}]")
    }

    override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant,
                                         remoteAudioTrackPublication: RemoteAudioTrackPublication) {
        Log.i(TAG, "onAudioTrackUnpublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                "name=${remoteAudioTrackPublication.trackName}]")
    }

    override fun onDataTrackPublished(remoteParticipant: RemoteParticipant,
                                      remoteDataTrackPublication: RemoteDataTrackPublication) {
        Log.i(TAG, "onDataTrackPublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                "name=${remoteDataTrackPublication.trackName}]")
    }

    override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant,
                                        remoteDataTrackPublication: RemoteDataTrackPublication) {
        Log.i(TAG, "onDataTrackUnpublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                "name=${remoteDataTrackPublication.trackName}]")
    }

    override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant,
                                       remoteVideoTrackPublication: RemoteVideoTrackPublication) {
        Log.i(TAG, "onVideoTrackPublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                "name=${remoteVideoTrackPublication.trackName}]")
    }

    override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant,
                                         remoteVideoTrackPublication: RemoteVideoTrackPublication) {
        Log.i(TAG, "onVideoTrackUnpublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                "name=${remoteVideoTrackPublication.trackName}]")
    }

    override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant,
                                        remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                        remoteAudioTrack: RemoteAudioTrack) {
        Log.i(TAG, "onAudioTrackSubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                "name=${remoteAudioTrack.name}]")
    }

    override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                          remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                          remoteAudioTrack: RemoteAudioTrack) {
        Log.i(TAG, "onAudioTrackUnsubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                "name=${remoteAudioTrack.name}]")
    }

    override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                twilioException: TwilioException) {
        Log.i(TAG, "onAudioTrackSubscriptionFailed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                "name=${remoteAudioTrackPublication.trackName}]" +
                "[TwilioException: code=${twilioException.code}, " +
                "message=${twilioException.message}]")
    }

    override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant,
                                       remoteDataTrackPublication: RemoteDataTrackPublication,
                                       remoteDataTrack: RemoteDataTrack) {
        Log.i(TAG, "onDataTrackSubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                "name=${remoteDataTrack.name}]")
        remoteDataTrack.setListener(dataListener)
    }

    override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                         remoteDataTrackPublication: RemoteDataTrackPublication,
                                         remoteDataTrack: RemoteDataTrack) {
        Log.i(TAG, "onDataTrackUnsubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                "name=${remoteDataTrack.name}]")
    }

    override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                               remoteDataTrackPublication: RemoteDataTrackPublication,
                                               twilioException: TwilioException) {
        Log.i(TAG, "onDataTrackSubscriptionFailed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                "name=${remoteDataTrackPublication.trackName}]" +
                "[TwilioException: code=${twilioException.code}, " +
                "message=${twilioException.message}]")
    }

    override fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant,
                                        remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                        remoteVideoTrack: RemoteVideoTrack) {
        Log.i(TAG, "onVideoTrackSubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                "name=${remoteVideoTrack.name}]")
        videoManager.addRemoteParticipantVideo(remoteVideoTrack)
    }

    override fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                          remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                          remoteVideoTrack: RemoteVideoTrack) {
        Log.i(TAG, "onVideoTrackUnsubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                "name=${remoteVideoTrack.name}]")
        videoManager.removeParticipantVideo(remoteVideoTrack)
    }

    override fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                                twilioException: TwilioException) {
        Log.i(TAG, "onVideoTrackSubscriptionFailed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                "name=${remoteVideoTrackPublication.trackName}]" +
                "[TwilioException: code=${twilioException.code}, " +
                "message=${twilioException.message}]")
    }

    override fun onAudioTrackEnabled(remoteParticipant: RemoteParticipant,
                                     remoteAudioTrackPublication: RemoteAudioTrackPublication) {
    }

    override fun onVideoTrackEnabled(remoteParticipant: RemoteParticipant,
                                     remoteVideoTrackPublication: RemoteVideoTrackPublication) {
    }

    override fun onVideoTrackDisabled(remoteParticipant: RemoteParticipant,
                                      remoteVideoTrackPublication: RemoteVideoTrackPublication) {
    }

    override fun onAudioTrackDisabled(remoteParticipant: RemoteParticipant,
                                      remoteAudioTrackPublication: RemoteAudioTrackPublication) {
    }
}

interface AVListener {
    fun remoteParticipantConnected()
    fun remoteParticipantDisconnected()
    fun onFailure()
}
