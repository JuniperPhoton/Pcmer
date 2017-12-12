package com.juniperphoton.pcmer

import android.Manifest
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PICK_FILE_REQUEST_CODE = 0
        private const val TAG = "MainActivity"

        const val STATUS_STOPPED = 0
        const val STATUS_PLAYING = 1
    }

    private var audioFileUri: Uri? = Uri.parse("/sdcard/raw.pcm")
    private var playbackTread: PlaybackThread? = null

    private var status: Int = STATUS_STOPPED

    private lateinit var channelCountEditText: EditText
    private lateinit var sampleRateEditText: EditText
    private lateinit var playbackButton: Button
    private lateinit var pickedText: TextView

    private lateinit var playDrawable: Drawable
    private lateinit var stopDrawable: Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        channelCountEditText = findViewById(R.id.channelCountEdit)
        sampleRateEditText = findViewById(R.id.sampleRateEdit)
        pickedText = findViewById(R.id.pickedText)

        prepareDrawables()

        findViewById<View>(R.id.pickButton).setOnClickListener {
            pick()
        }

        playbackButton = findViewById(R.id.playButton)
        playbackButton.setOnClickListener {
            onClickPlaybackButton()
        }

        updateFileUriDisplay()

        playbackTread = PlaybackThread(this)
        playbackTread!!.name = "PlaybackThread"
        playbackTread!!.start()

        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
    }

    private fun updateFileUriDisplay() {
        pickedText.text = audioFileUri?.toString() ?: "EMPTY"
    }

    private fun prepareDrawables() {
        playDrawable = ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp)
        stopDrawable = ContextCompat.getDrawable(this, R.drawable.ic_stop_black_24dp)

        playDrawable.setBounds(0, 0, playDrawable.minimumWidth, playDrawable.minimumHeight)
        stopDrawable.setBounds(0, 0, stopDrawable.minimumWidth, stopDrawable.minimumHeight)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        intent?.data?.let {
            audioFileUri = it
        }
        updateFileUriDisplay()
    }

    override fun onPause() {
        stop()
        super.onPause()
    }

    private fun pick() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "file/*"
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    private fun onClickPlaybackButton() {
        if (audioFileUri == null) {
            toast("Please pick a file first")
            return
        }
        when (status) {
            STATUS_STOPPED -> {
                play()
            }
            STATUS_PLAYING -> {
                stop()
            }
        }
    }

    private fun play() {
        playbackButton.setCompoundDrawables(stopDrawable, null, null, null)
        playbackButton.text = "STOP"
        status = STATUS_PLAYING
        val config = PlaybackThread.Config(
                audioFileUri!!, sampleRateEditText.text.toString().toInt(),
                channelCountEditText.text.toString().toInt())
        val msg = Message.obtain()
        msg.what = PlaybackThread.MESSAGE_START
        msg.obj = config
        playbackTread?.handlerRef?.get()?.sendMessage(msg)
    }

    private fun stop() {
        playbackButton.setCompoundDrawables(playDrawable, null, null, null)
        playbackButton.text = "PLAY"
        status = STATUS_STOPPED
        playbackTread?.requestStop = true
    }

    private fun toast(str: String?) {
        val content = str ?: return
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
    }
}
