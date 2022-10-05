package com.sorrybro.hapticgenerator

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.media.MediaPlayer
import android.media.audiofx.HapticGenerator
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var browse: Button
    private lateinit var play: Button
    private lateinit var textView: TextView
    private lateinit var fileName: TextView
    private var hapticGenerator: HapticGenerator? = null
    private var player: MediaPlayer? = null
    private var fileUri: Uri? = null
    private var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun isBatterySaverOn(): Boolean {
        val powerManager: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    override fun onStart() {
        super.onStart()
        browse = findViewById(R.id.browse)
        play = findViewById(R.id.play)
        textView = findViewById(R.id.label)
        fileName = findViewById(R.id.file)
        if (HapticGenerator.isAvailable()) {
            textView.setText(R.string.haptic_supported)
            textView.setTextColor(Color.GREEN)
        } else {
            textView.setText(R.string.haptic_not_supported)
            textView.setTextColor(Color.RED)
        }
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // There are no request codes
                    fileUri = result.data?.data
                    fileName.text = getFileName(fileUri!!)
                }
            }
        browse.setOnClickListener {
            val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
            chooseFile.type = "audio/*"
            val intent = Intent.createChooser(chooseFile, "Choose a file")
            resultLauncher.launch(intent)
        }
        play.setOnClickListener {
            if (isBatterySaverOn()) {
                Toast.makeText(this, getString(R.string.power_save_on), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (playing) {
                releasePlayer()
                playing = false
                play.text = getString(R.string.play)
                browse.isEnabled = true
            } else {
                fileUri?.let { it1 -> play(it1) }
                playing = true
                play.text = getString(R.string.pause)
                browse.isEnabled = false
            }
        }
    }

    @SuppressLint("Range")
    fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    private fun releasePlayer() {
        player?.release()
        hapticGenerator?.release()
    }

    private fun play(file: Uri) {
        browse.isEnabled = false
        releasePlayer()
        if (HapticGenerator.isAvailable()) {
            player = MediaPlayer.create(applicationContext, file)
            hapticGenerator = HapticGenerator.create(player!!.audioSessionId)
            hapticGenerator?.enabled = true
        }
        player?.setOnCompletionListener {
            browse.isEnabled = true
        }
        player?.start()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}