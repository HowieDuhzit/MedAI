package com.example.medai.shared

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.medai.shared.voice.ConversationManager
import com.example.medai.shared.voice.ConversationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.ArrayList

class MyMusicService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat
    private lateinit var conversationManager: ConversationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            // In our AI app, 'Play' means 'Start Listening'
            conversationManager.startListening()
        }

        override fun onPause() {
            conversationManager.stopConversation()
        }

        override fun onStop() {
            conversationManager.stopConversation()
        }
    }

    override fun onCreate() {
        super.onCreate()

        conversationManager = ConversationManager(this)
        conversationManager.initialize()

        session = MediaSessionCompat(this, "MyMusicService")
        sessionToken = session.sessionToken
        session.setCallback(callback)
        session.isActive = true

        observeConversationState()
    }

    private fun observeConversationState() {
        serviceScope.launch {
            conversationManager.state.collect { state ->
                updatePlaybackState(state)
            }
        }
    }

    private fun updatePlaybackState(state: ConversationState) {
        val stateBuilder = PlaybackStateCompat.Builder()
        
        val playbackState = when (state) {
            ConversationState.LISTENING -> PlaybackStateCompat.STATE_BUFFERING
            ConversationState.PROCESSING -> PlaybackStateCompat.STATE_CONNECTING
            ConversationState.SPEAKING -> PlaybackStateCompat.STATE_PLAYING
            ConversationState.IDLE -> PlaybackStateCompat.STATE_PAUSED
            ConversationState.ERROR -> PlaybackStateCompat.STATE_ERROR
        }

        val actions = PlaybackStateCompat.ACTION_PLAY or 
                     PlaybackStateCompat.ACTION_PAUSE or 
                     PlaybackStateCompat.ACTION_STOP

        stateBuilder.setState(playbackState, 0, 1.0f)
        stateBuilder.setActions(actions)
        
        session.setPlaybackState(stateBuilder.build())
    }

    override fun onDestroy() {
        conversationManager.destroy()
        session.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot("root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
        val mediaItems = ArrayList<MediaItem>()

        if ("root" == parentId) {
            val description = MediaDescriptionCompat.Builder()
                .setMediaId("assistant_trigger")
                .setTitle("Tap to Talk to MedAI")
                .setSubtitle("Voice Assistant")
                .build()
            mediaItems.add(MediaItem(description, MediaItem.FLAG_PLAYABLE))
        }

        result.sendResult(mediaItems)
    }
}
