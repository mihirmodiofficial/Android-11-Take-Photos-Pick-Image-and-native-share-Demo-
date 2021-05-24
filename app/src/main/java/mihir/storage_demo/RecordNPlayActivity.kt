package mihir.storage_demo

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import mihir.storage_demo.databinding.ActivityRecordNPlayBinding
import mihir.storage_demo.permissionUtils.OnPermissionDeniedListener
import mihir.storage_demo.permissionUtils.OnPermissionGrantedListener
import mihir.storage_demo.permissionUtils.OnPermissionPermanentlyDeniedListener
import java.io.File
import java.io.FileDescriptor

class RecordNPlayActivity : BaseActivity(), OnPermissionGrantedListener,
    OnPermissionDeniedListener,OnPermissionPermanentlyDeniedListener {


    lateinit var binding: ActivityRecordNPlayBinding
    var mediaRecorder: MediaRecorder? = null
    var mediaPlayer: MediaPlayer? = null
    var audioUri: Uri? = null
    var currentPosition: Int = 0
    var isRecordButtonClicked: Boolean = false
    var isRecordingStopped: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.buttonRecordAudio.setOnClickListener {
            isRecordButtonClicked = true
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    onPermissionGrantedListener.OnPermissionGranted()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false -> {
                    onPermissionPermanentlyDeniedListener.OnPermissionPermanentlyDenied()
                }
                else -> {
                    checkSinglePermission(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        binding.buttonStopRecording.setOnClickListener {
            isRecordButtonClicked = false
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    onPermissionGrantedListener.OnPermissionGranted()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false -> {
                    onPermissionPermanentlyDeniedListener.OnPermissionPermanentlyDenied()
                }
                else -> {
                    checkSinglePermission(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        binding.buttonPlayAudio.setOnClickListener {
            playAudio()
        }
        binding.buttonPauseAudio.setOnClickListener {
            pauseAudio()
        }
        binding.buttonResumeAudio.setOnClickListener {
            resumeAudio()
        }
        binding.buttonStopAudio.setOnClickListener {
            stopPlayingAudio()
        }
        onPermissionDeniedListener = this
        onPermissionGrantedListener = this
        onPermissionPermanentlyDeniedListener = this

    }

    private fun resumeAudio() {
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            mediaPlayer?.seekTo(currentPosition)
            Toast.makeText(this, "Recording playing resumed", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopPlayingAudio() {
        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                currentPosition = 0
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                Toast.makeText(this, "Recording playing stopped", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pauseAudio() {
        if (mediaPlayer != null) {
            if (mediaPlayer?.isPlaying!!) {
                currentPosition = mediaPlayer?.currentPosition!!
                mediaPlayer?.pause()
                Toast.makeText(this, "Recording playing paused", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun playAudio() {
        if (isRecordingStopped) {
            try {
                mediaPlayer = MediaPlayer()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    mediaPlayer!!.setDataSource(getFilePath())
                    //00445262
                } else {
                    mediaPlayer!!.setDataSource(getFileDescriptor2())
                }
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()
                Toast.makeText(this, "Recording is playing", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecordingStopped = true
            Toast.makeText(this, "Recording is stopped", Toast.LENGTH_LONG).show()
        }
    }


    override fun findContentView(): Int {
        return R.layout.activity_record_n_play
    }

    override fun bindViewWithViewBinding(view: View) {
        binding = ActivityRecordNPlayBinding.bind(view)
    }

    override fun OnPermissionGranted() {
        if (isRecordButtonClicked) {
            if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                recordAudio()
            } else {
                Toast.makeText(this, "No Microphone available on this device.", Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                stopRecording()
            } else {
                Toast.makeText(this, "No Microphone available on this device.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun recordAudio() {
        try {
            isRecordingStopped = false
            mediaRecorder = MediaRecorder()
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                mediaRecorder!!.setOutputFile(getFilePath())
            } else {
                mediaRecorder!!.setOutputFile(getFileDescriptor())
            }
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder!!.prepare()
            mediaRecorder!!.start()
            Toast.makeText(this, "Recording is started", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileDescriptor(): FileDescriptor {
        var displayName: String? = null
        var relativePath: String? = null
        var media_id: String? = null
        val filePathColumn =
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
        var uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("test_mihir_audio.mp3")
        var cursor: Cursor? = null
        try {
            cursor = getContentResolver().query(uri, filePathColumn, selection, selectionArgs, null)
            if (cursor != null) {
                //cursor.moveToFirst()
                while (cursor.moveToNext()) {
                    var idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    var nameColumn: Int =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val uri: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    relativePath =
                        ContentUris.withAppendedId(uri, cursor.getLong(idColumn)).toString()
                    displayName = cursor.getString(nameColumn)
                    media_id = cursor.getLong(idColumn).toString()

                }
            }
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }


        mimeType = "audio/*"
        var resolver: ContentResolver = applicationContext.contentResolver
        var audioCollection: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "test_mihir_audio.mp3")
            //put(MediaStore.Audio.Media.TITLE, "test_audio")
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, getAudioDirectoryPath())
            //put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        if (!relativePath.isNullOrEmpty()) {
            audioUri = Uri.parse(relativePath)
        } else {
            audioUri = resolver.insert(audioCollection, values)
        }
        var parcelFileDescriptor: ParcelFileDescriptor =
            resolver.openFileDescriptor(audioUri!!, "wt")!!
        return parcelFileDescriptor.fileDescriptor
    }

    private fun getFileDescriptor2(): FileDescriptor {
        var parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(audioUri!!, "r")!!
        return parcelFileDescriptor.fileDescriptor
    }

    private fun getFilePath(): String {
        var directory: File? =
            getAppSpecificAlbumStorageDir(this, Environment.DIRECTORY_MUSIC, "MihirDemo")
        var file: File = File(directory, "test_audio.mp3")
        return file.absolutePath
    }

    override fun OnPermissionDenied() {
        ShowPrompt(false)
    }

    override fun onPause() {
        if(!isRecordingStopped){
            stopRecording()
        }else{
            stopPlayingAudio()
        }
        super.onPause()
    }

    override fun OnPermissionPermanentlyDenied() {
        ShowPrompt(true)

    }
}