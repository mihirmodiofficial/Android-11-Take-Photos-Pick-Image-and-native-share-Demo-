package mihir.storage_demo

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import mihir.storage_demo.BuildConfig
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
import mihir.storage_demo.databinding.ActivityMainBinding
import mihir.storage_demo.permissionUtils.OnPermissionDeniedListener
import mihir.storage_demo.permissionUtils.OnPermissionGrantedListener
import mihir.storage_demo.permissionUtils.OnPermissionPermanentlyDeniedListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*

class MainActivity : BaseActivity(), OnActivityResultListener,
    OnPermissionPermanentlyDeniedListener, OnPermissionGrantedListener, OnPermissionDeniedListener,View.OnClickListener {

    lateinit var binding: ActivityMainBinding
    var button_click_code: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.buttonCamera.setOnClickListener(this)
        binding.buttonCamera2.setOnClickListener(this)
        binding.buttonGallery.setOnClickListener(this)
        binding.buttonGallery2.setOnClickListener(this)
        binding.buttonPdf.setOnClickListener(this)
        buttonRecordAudio.setOnClickListener(this)
        buttonShare.setOnClickListener(this)
        buttonuploadImage.setOnClickListener(this)
        buttonDownloadImage.setOnClickListener(this)
        buttonSAF.setOnClickListener(this)
        onActivityResultListener = this
        onPermissionDeniedListener = this
        onPermissionGrantedListener = this
        onPermissionPermanentlyDeniedListener = this
    }


    private fun uploadImage() {
        if(!shareUri.toString().isNullOrEmpty()){
            Log.d("34245452", shareUri.toString())
            var image: Bitmap = getBitmapFromContentResolver(this!!.shareUri!!)

            var orientation: Int = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                getOrientation2(this!!.shareUri!!)
            }else{
                getOrientation(this!!.shareUri!!)
            }

            val file = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                createImageFile()
            }else{
                File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}" + File.separator + "MihirDemo",getNewFileName())
            }
            var fos: FileOutputStream = FileOutputStream(file)
            var bitmap = image

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

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            callUploadApi(file)

        }
    }


    override fun findContentView(): Int {
        return R.layout.activity_main
    }

    override fun bindViewWithViewBinding(view: View) {
        binding = ActivityMainBinding.bind(view)
    }


    private fun openNativeShare() {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, shareUri)
            type = mimeType
        }
        startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))

    }

    private fun openPdf() {
        try {
            mimeType = "application/pdf"
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
            }
            initRequestCode(intent, PICK_PDF_FILE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "${e.message}", Toast.LENGTH_LONG).show()
        }


    }

    private fun openGallery(requestCode: Int) {
        mimeType = "image/*"

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }


        try {
            val intent =
                Intent(Intent.ACTION_PICK, collection).apply {
                    type = mimeType
                }
            intent.resolveActivity(packageManager)?.also {
                initRequestCode(intent, requestCode)
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun clickPhoto(requestCode: Int) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.also {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "${ex.message}", Toast.LENGTH_LONG).show()
                null
            }
            // Continue only if the File was successfully created
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    it
                )

                mimeType = "image/*"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, getNewFileName())
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        getImageDirectoryPath()
                    )
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                } else {
                    val imageUri =
                        contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), values)
                    if (imageUri != null) {
                        currentPhotoPath = imageUri.toString()
                        shareUri = imageUri
                    }
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                }
                initRequestCode(takePictureIntent, requestCode)

            }
        }
    }

    private fun initRequestCode(takePictureIntent: Intent, requestImageCapture: Int) {
        currentRequestCode = requestImageCapture
        startActivityForResult.launch(takePictureIntent)
    }


    override fun onActivityResult(
        result: ActivityResult,
        currentRequestCode: Int
    ) {

        if (currentRequestCode == REQUEST_IMAGE_CAPTURE_WITHOUT_SCALE) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                var file: File = File(currentPhotoPath)
                shareUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )
                binding.imageViewProfile.load(currentPhotoPath)
            } else {
                var image: Bitmap = getBitmapFromContentResolver(Uri.parse(currentPhotoPath))
                imageViewProfile.load(currentPhotoPath)
            }
            //to show image in gallery
            addImageInGallery()
        }else if (currentRequestCode == REQUEST_IMAGE_CAPTURE_WITH_SCALE) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                var file: File = File(currentPhotoPath)
                shareUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )
                scaleImage()
                binding.imageViewProfile.load(currentPhotoPath)
            } else {
                scaleImage2()
                var image: Bitmap = getBitmapFromContentResolver(Uri.parse(currentPhotoPath))
                imageViewProfile.load(currentPhotoPath)
            }
            //to show image in gallery
            addImageInGallery()
        }
        else if (currentRequestCode == PICK_IMG_FILE_WITH_COMPRESSION) {
            result?.data?.data?.also { uri ->
                shareUri = uri
                resizeImage(uri)
                binding.imageViewProfile.load(shareUri.toString())
            }
        } else if (currentRequestCode == PICK_IMG_FILE_WITHOUT_COMPRESSION) {
            result?.data?.data?.also { uri ->
                shareUri = uri
                binding.imageViewProfile.load(shareUri.toString())
            }
        } else if (currentRequestCode == PICK_PDF_FILE) {
            mimeType = "application/pdf"
            result?.data?.data?.also { uri ->
                shareUri = uri
                val browserIntent = Intent(Intent.ACTION_VIEW)
                browserIntent.setDataAndType(shareUri, mimeType)
                browserIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
                startActivity(browserIntent)
            }
        } else if (currentRequestCode == WRITE_REQUEST_CODE) {
            Toast.makeText(this, "Request Granted", Toast.LENGTH_LONG).show()
        }

    }

    private fun resizeImage(uri: Uri) {
        alterDocument(uri)
    }


    override fun OnPermissionPermanentlyDenied() {
        ShowPrompt(true)
    }

    override fun OnPermissionGranted() {
        if (button_click_code == 1) {
            openGallery(PICK_IMG_FILE_WITH_COMPRESSION)
        } else if (button_click_code == 2) {
            openGallery(PICK_IMG_FILE_WITHOUT_COMPRESSION)
        } else if(button_click_code == 4){
            var intent: Intent = Intent(this,RecordNPlayActivity::class.java)
            startActivity(intent)
        }
        else {
            openPdf()
        }
    }

    override fun OnPermissionDenied() {
        ShowPrompt(false)
    }



    private fun scaleImage() {
        // Get the dimensions of the View
        val targetW: Float = 1920.0f//1280.0f;//816.0f;
        val targetH: Float = 1080.0f//852.0f;//612.0f;

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Float = Math.max(1f, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor.toInt()
            inPurgeable = true
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            var orientation: Int = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                getOrientation2(this!!.shareUri!!)
            }else{
                getOrientation(this!!.shareUri!!)
            }
            binding.imageViewProfile.setImageBitmap(bitmap)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                saveImage(bitmap, orientation,currentPhotoPath)
            }else{
                creatNewImageFile(bitmap,orientation,currentPhotoPath)
            }

        }
    }

    private fun scaleImage2() {
        var inputStream: InputStream =
            contentResolver.openInputStream(this!!.shareUri!!)!!

        val parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(this!!.shareUri!!, "r")!!
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor


        // Get the dimensions of the View
        val targetW: Float = 1920.0f//1280.0f;//816.0f;
        val targetH: Float = 1080.0f//852.0f;//612.0f;

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFileDescriptor(fileDescriptor,null, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Float = Math.max(1f, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor.toInt()
            inPurgeable = true
        }
        BitmapFactory.decodeFileDescriptor(fileDescriptor,null, bmOptions)?.also { bitmap ->
            var orientation: Int = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                getOrientation2(this!!.shareUri!!)
            }else{
                getOrientation(this!!.shareUri!!)
            }
            binding.imageViewProfile.setImageBitmap(bitmap)
            creatNewImageFile(bitmap,orientation,currentPhotoPath)

        }
    }

    override fun onClick(view: View?) {
        when(view){
            binding.buttonCamera -> {
                if(this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
                    clickPhoto(REQUEST_IMAGE_CAPTURE_WITHOUT_SCALE)
                }else{
                    Toast.makeText(this,"No camera available on this device.",Toast.LENGTH_LONG).show()
                }
            }
            binding.buttonCamera2->{
                if(this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    clickPhoto(REQUEST_IMAGE_CAPTURE_WITH_SCALE)
                }else{
                    Toast.makeText(this,"No camera available on this device.",Toast.LENGTH_LONG).show()
                }
            }
            binding.buttonGallery -> {
                button_click_code = 1
                checkPermissions()
            }
            binding.buttonGallery2 -> {
                button_click_code = 2
                checkPermissions()
            }
            binding.buttonPdf -> {
                button_click_code = 3
                checkPermissions()
            }
            binding.buttonRecordAudio -> {
                if(this.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)){
                    button_click_code = 4
                    checkPermissions()
                }else{
                    Toast.makeText(this,"No microphone available on this device.",Toast.LENGTH_LONG).show()
                }
            }
            binding.buttonShare -> {
                if (!shareUri.toString().isNullOrEmpty()) {
                    openNativeShare()
                }else{
                    Toast.makeText(this@MainActivity,"Please select image",Toast.LENGTH_LONG).show()
                }
            }
            binding.buttonuploadImage -> {
                if (mimeType.startsWith("image", true)) {
                    showLoader()
                    uploadImage()
                }else{
                    Toast.makeText(this@MainActivity,"Please select image",Toast.LENGTH_LONG).show()
                }
            }
            binding.buttonDownloadImage -> {
                if (!editTextDownloadUrl.text.isNullOrEmpty()) {
                    downLoadImage(editTextDownloadUrl.text.toString())
                } else {
                    Toast.makeText(this, "Please enter image url", Toast.LENGTH_LONG).show()
                }
            }
            binding.buttonSAF -> {
                var intent: Intent = Intent(this,SAFDemoActivty::class.java)
                startActivity(intent)
            }
        }
    }

}
