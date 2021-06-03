package com.plcoding.spotifycloneyt.exoPlayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.plcoding.spotifycloneyt.other.Constants.NETWORK_ERROR
import com.plcoding.spotifycloneyt.other.Event
import com.plcoding.spotifycloneyt.other.Resource


class MusicServiceConnection(
    context: Context
) {
    /*
           private MutableLiveData для того, чтобы можно было вносить изменения только с этого класса
           public LiveData для того, чтобы можно было обсервить этот LiveData

           3:00 https://www.youtube.com/watch?v=-ClM1SO_Dp8&list=PLQkwcJG4YTCT-lTlkOmE-PpRkuyNXq6sW&index=11&ab_channel=PhilippLacknerPhilippLackner
     */
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    private val _curPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val curPlayingSong: LiveData<MediaMetadataCompat?> = _curPlayingSong

    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(
                context,
                MusicService::class.java
            ),
            mediaBrowserConnectionCallback,
            null
    ).apply {
        connect()
    }
        // skip song, pause, and so on
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

        // start subscribe a mediaId to get access to MediaItems
    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            _isConnected.postValue(Event(Resource.error(
                "The connection was suspended",
                false
            )))
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            _isConnected.postValue(Event(Resource.error(
                "Couldn't connect to MediaBrowser",
                false
            )))
        }
    }


    private inner class MediaControllerCallback: MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
           _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _curPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when(event){
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check internet connection",
                            null
                        )
                    )
                )
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

}