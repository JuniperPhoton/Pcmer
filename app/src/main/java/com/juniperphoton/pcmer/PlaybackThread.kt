package com.juniperphoton.pcmer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference

class PlaybackThread(context: Context) : Thread() {
    companion object {
        const val MESSAGE_START = 0

        private const val TAG = "PlaybackThread"
    }

    private var contextRef: WeakReference<Context>? = null

    @Volatile
    var requestStop = false

    var handlerRef: WeakReference<PlaybackHandler>? = null

    init {
        contextRef = WeakReference(context)
    }

    private var track: AudioTrack? = null

    override fun run() {
        Looper.prepare()
        handlerRef = WeakReference(PlaybackHandler(this))
        Looper.loop()
    }

    private fun config(config: Config): Boolean {
        val attrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

        var channelIndexMask = when (config.channelCount) {
            1 -> {
                0x01
            }
            2 -> {
                0x11
            }
            else -> {
                throw IllegalArgumentException("Channel count which is greater than 2 is not supported yet")
            }
        }

        val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(config.sampleRate)
                .setChannelIndexMask(channelIndexMask)
                .build()
        val bufferSize = AudioTrack.getMinBufferSize(config.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)

        try {
            track = AudioTrack(attrs, format, bufferSize, AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE)
            return true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        return false
    }

    private fun startPlaying(config: Config) {
        Log.i(TAG, "startPlaying: $config")

        val context = contextRef?.get() ?: return

        if (!config(config)) {
            Log.e(TAG, "failed to config")
        }

        requestStop = false

        val uri = config.uri
        var inputStream = if (uri.toString().startsWith("content")) {
            context.contentResolver.openInputStream(uri)
        } else {
            FileInputStream(File(uri.toString()))
        }

        inputStream.use {
            val buffer = ByteArray(4068)
            var read = 1
            while (read > 0 && !requestStop) {
                read = inputStream.read(buffer, 0, buffer.size)
                track?.write(buffer, 0, buffer.size)
                track?.play()
            }
        }

        Log.d(TAG, "the end")

        if (requestStop) {
            stopPlaying()
            track = null
        }
    }

    private fun stopPlaying() {
        Log.i(TAG, "stopPlaying")

        requestStop = true
        try {
            track?.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    class PlaybackHandler(thread: PlaybackThread) : Handler() {
        private var threadRef: WeakReference<PlaybackThread>? = null

        init {
            threadRef = WeakReference(thread)
        }

        override fun handleMessage(msg: Message) {
            Log.i(TAG, "handle message: ${msg.what}")
            val t = threadRef?.get() ?: return
            when (msg.what) {
                MESSAGE_START -> {
                    t.startPlaying(msg.obj as Config)
                }
            }
        }
    }

    data class Config(val uri: Uri,
                      val sampleRate: Int, val channelCount: Int)
}