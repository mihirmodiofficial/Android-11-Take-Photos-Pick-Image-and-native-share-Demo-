package mihir.storage_demo

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.documentfile.provider.DocumentFile
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import mihir.storage_demo.permissionUtils.OnPermissionDeniedListener
import mihir.storage_demo.permissionUtils.OnPermissionGrantedListener
import mihir.storage_demo.permissionUtils.OnPermissionPermanentlyDeniedListener
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(findContentView())
        bindViewWithViewBinding((findViewById<ViewGroup>(android.R.id.content)).getChildAt(0))
        createAlertDialog()
    }

    var shareUri: Uri?=null


    lateinit var onActivityResultListener: OnActivityResultListener
    lateinit var onPermissionDeniedListener: OnPermissionDeniedListener
    lateinit var onPermissionGrantedListener: OnPermissionGrantedListener
    lateinit var onPermissionPermanentlyDeniedListener: OnPermissionPermanentlyDeniedListener

    val REQUEST_IMAGE_CAPTURE_WITHOUT_SCALE = 1
    val PICK_IMG_FILE_WITH_COMPRESSION = 2
    val PICK_IMG_FILE_WITHOUT_COMPRESSION = 3
    val PICK_PDF_FILE = 4
    val WRITE_REQUEST_CODE = 5
    var currentRequestCode = 0
    val REQUEST_IMAGE_CAPTURE_WITH_SCALE = 6
    var mimeType: String = ""
    lateinit var currentPhotoPath: String

    lateinit var dialog: AlertDialog

    @LayoutRes
    abstract fun findContentView(): Int

    abstract fun bindViewWithViewBinding(view: View)


    fun getImageDirectoryPath(): String{
        return Environment.DIRECTORY_PICTURES + File.separator + "MihirDemo"
    }

    fun getAudioDirectoryPath(): String{
        return Environment.DIRECTORY_MUSIC + File.separator + "MihirDemo" + File.separator
    }


    fun getNewFileName() : String {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
       return "JPEG_${timeStamp}_.jpg"
    }

    fun ShowPrompt(isPermanentlyDenied: Boolean) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission necessary")
        alertBuilder.setMessage("Allow this app to access Photos and videos?")
        alertBuilder.setPositiveButton(
            android.R.string.yes,
            object : DialogInterface.OnClickListener {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                override fun onClick(dialog: DialogInterface, which: Int) {
                    //hasPermissions(context,permissions);
                    if (isPermanentlyDenied) {
                        showSettings()
                    } else {
                        checkPermissions()
                    }
                }
            })
        val alert = alertBuilder.create()
        alert.show()
    }

     fun checkSinglePermission(permission: String) {
        requestSinglePermission.launch(permission)
    }

    fun checkPermissions() {
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
            )
        )

    }

    val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var isDenied = 2
            permissions.entries.forEach {
                if (it.value == false) {
                    var showRationale1: Boolean = shouldShowRequestPermissionRationale(it.key)
                    if (!showRationale1) {
                        isDenied = 0
                    } else {
                        isDenied = 1
                    }
                }
                Log.e("DEBUG", "${it.key} = ${it.value}")
            }


            //ShowPrompt(context,permissions);
            if (isDenied == 1) onPermissionDeniedListener.OnPermissionDenied()
            else if (isDenied == 0) onPermissionPermanentlyDeniedListener.OnPermissionPermanentlyDenied()
            else onPermissionGrantedListener.OnPermissionGranted()

        }


    val requestSinglePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            var isDenied = 2
            if(isGranted){

            }else{
                isDenied = 1
            }

            //ShowPrompt(context,permissions);
            if (isDenied == 1) onPermissionDeniedListener.OnPermissionDenied()
            else if (isDenied == 0) onPermissionPermanentlyDeniedListener.OnPermissionPermanentlyDenied()
            else onPermissionGrantedListener.OnPermissionGranted()

        }



    fun showSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    val startSenderForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                onActivityResultListener.onActivityResult(result, currentRequestCode)
            }
        }


    val startActivityForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                onActivityResultListener.onActivityResult(result, currentRequestCode)
            }
        }

    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getAppSpecificAlbumStorageDir(this, Environment.DIRECTORY_PICTURES,"MihirDemo")
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun getAppSpecificAlbumStorageDir(context: Context, albumName: String,subAlbumName: String): File? {
        // Get the pictures directory that's inside the app-specific directory on
        // external storage.
        val file = File(
            context.getExternalFilesDir(
                albumName
            ), subAlbumName
        )
        if (!file?.mkdirs()) {
            Log.e("fssfsf", "Directory not created")
        }

        return file
    }


    fun AppCompatImageView.load(strImage: String) {
        Glide.with(this.context)
            .asDrawable()
            .load(strImage)
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(this)
    }

    fun alterDocument(uri: Uri) {
       var image: Bitmap = getBitmapFromContentResolver(uri)
        var orientation = getOrientation(uri)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            saveImage(image, orientation)
        }else{
            creatNewImageFile(image,orientation)
        }
    }

     fun getBitmapFromContentResolver(shareUri: Uri): Bitmap {
        val parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(shareUri, "r")!!
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return  image
    }


    fun getOrientation(shareUri: Uri): Int {
        val orientationColumn = arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cur =
            contentResolver.query(
                shareUri,
                orientationColumn,
                null,
                null,
                null
            )
        var orientation = -1
        if (cur != null && cur.moveToFirst()) {
            if(cur.columnCount > 0){
                orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]))
            }
            cur.close()
        }
        return  orientation
    }

    @SuppressLint("NewApi")
    fun getOrientation2(shareUri: Uri): Int {
        var inputStream: InputStream =
            contentResolver.openInputStream(shareUri)!!
        return  getOrientation3(inputStream)
    }

    @SuppressLint("NewApi")
    private fun getOrientation3(inputStream: InputStream): Int {
        val exif: ExifInterface
        var orientation: Int = -1
        try {
            exif = ExifInterface(inputStream)

            orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, 0
            )
            Log.d("EXIF", "Exif: $orientation")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return  orientation

    }


     fun saveImage(image: Bitmap, orientation: Int) {
        var bitmap = image
        try {

            val file = createImageFile()
            val outputStream = BufferedOutputStream(FileOutputStream(file))


            if (orientation != -1 && orientation != 0) {

                val matrix = Matrix()
                matrix.postRotate(orientation.toFloat())
                bitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height, matrix,
                    true
                )
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    fun saveImage(image: Bitmap, orientation: Int,filePath: String) {
        var bitmap = image
        try {

            val file = File(filePath)
            val outputStream = BufferedOutputStream(FileOutputStream(file))


            if (orientation != -1 && orientation != 0) {

                val matrix = Matrix()
                if (orientation == 6) {
                    matrix.postRotate(90f)
                    Log.d("EXIF", "Exif: $orientation")
                } else if (orientation == 3) {
                    matrix.postRotate(180f)
                    Log.d("EXIF", "Exif: $orientation")
                } else if (orientation == 8) {
                    matrix.postRotate(270f)
                    Log.d("EXIF", "Exif: $orientation")
                }else{
                    matrix.postRotate(orientation.toFloat())
                }
                bitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height, matrix,
                    true
                )
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }


    fun creatNewImageFile(image: Bitmap,orientation: Int) {
        mimeType = "image/*"
        var bitmap: Bitmap = image
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, getNewFileName())
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                getImageDirectoryPath()
            )
        }
        val imageUri =
            contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), values)
        var fos: FileOutputStream =
            contentResolver.openOutputStream(Objects.requireNonNull(imageUri)!!) as FileOutputStream

        if (orientation != -1 && orientation != 0) {

            val matrix = Matrix()
            matrix.postRotate(orientation.toFloat())
            bitmap = Bitmap.createBitmap(
                image, 0, 0,
                bitmap.width, bitmap.height, matrix,
                true
            )
        }


        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
        Objects.requireNonNull<OutputStream?>(fos)

        if (imageUri != null) {
            shareUri = imageUri
        }
    }

    fun creatNewImageFile(image: Bitmap,orientation: Int,filePath: String) {
        mimeType = "image/*"
        var bitmap: Bitmap = image
        var fos: FileOutputStream =
            contentResolver.openOutputStream(Objects.requireNonNull(Uri.parse(filePath))!!,"wt") as FileOutputStream

        if (orientation != -1 && orientation != 0) {

            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90f)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 3) {
                matrix.postRotate(180f)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 8) {
                matrix.postRotate(270f)
                Log.d("EXIF", "Exif: $orientation")
            }else{
                matrix.postRotate(orientation.toFloat())
            }
            bitmap = Bitmap.createBitmap(
                image, 0, 0,
                bitmap.width, bitmap.height, matrix,
                true
            )
        }


        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
        Objects.requireNonNull<OutputStream?>(fos)
    }


    fun addImageInGallery() {
        val file = File(currentPhotoPath)
        MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath),
            null, MediaScannerConnection.OnScanCompletedListener { path, uri ->
                Log.i("ExternalStorage", "Scanned " + path + ":");
                Log.i("ExternalStorage", "-> uri=" + uri);
            })
    }


    fun callUploadApi(file: File) {
        val requestFile: RequestBody = file.asRequestBody("image/*".toMediaType())
        val body: MultipartBody.Part =
            MultipartBody.Part.createFormData("image", "test.jpg", requestFile)
        val apiService: ApiService = Config.getClient().create(ApiService::class.java)
        var call: Call<ResponseBody> = apiService.getUpdateProfileImage(body,"","Ios")
        call.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                hideLoader()
                Toast.makeText(this@BaseActivity,"${t.message}", Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                hideLoader()
                Toast.makeText(this@BaseActivity,"Image uploaded", Toast.LENGTH_LONG).show()
            }

        })

    }


    fun downLoadImage(downLoadUrl: String) {
        showLoader()
        val apiService: ApiService = Config.getClient().create(ApiService::class.java)
        var call: Call<ResponseBody> = apiService.downloadFileWithDynamicUrlSync(downLoadUrl)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                hideLoader()
                Toast.makeText(this@BaseActivity,"${t.message}", Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                hideLoader()
                if (response.code() == 200) {
                    writeResponseBodyToDisk(response.body())
                } else {
                    Toast.makeText(this@BaseActivity,"server contact failed.", Toast.LENGTH_LONG).show()
                }
            }

        })

    }

    private fun writeResponseBodyToDisk(body: ResponseBody?) {
        if (body != null) {
           var inputStream: InputStream =  body.byteStream()
           var bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                saveImage(bitmap,0)
            }else{
                creatNewImageFile(bitmap,0)
            }
            Toast.makeText(this@BaseActivity,"File Downloaded.", Toast.LENGTH_LONG).show()

        }
    }


    fun createAlertDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialog)
        dialog = builder.create()
    }
    fun showLoader(){
        dialog.show()
    }
    fun hideLoader(){
        dialog.dismiss()
    }



}