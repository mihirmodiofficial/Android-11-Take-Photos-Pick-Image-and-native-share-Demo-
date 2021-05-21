import android.app.ProgressDialog
import android.app.RecoverableSecurityException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat.startActivity
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.UnsupportedEncodingException


/*
package com.example.storage_access_framework_demo

import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import java.io.File
import java.io.FileOutputStream

var displayName: String? = null
var media_id: String? = null


if (uri.toString().startsWith("content://")) {
    val filePathColumn = arrayOf(MediaStore.Images.Media._ID)
    var cursor: Cursor? = null
    try {
        cursor = getContentResolver().query(uri, filePathColumn, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            var columnIndex: Int = cursor.getColumnIndexOrThrow(filePathColumn[0])
            val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            displayName =
                ContentUris.withAppendedId(uri, cursor.getLong(columnIndex))
                    .toString()
            media_id = cursor.getLong(columnIndex).toString()
        }
    } finally {
        if (cursor != null) {
            cursor.close()
        }
    }

    if (!displayName.isNullOrEmpty()) {

        var list: ArrayList<Uri> = ArrayList()
        list.add(Uri.parse(displayName))


        if (checkUriPermission(
                Uri.parse(displayName),
                Binder.getCallingPid(),
                Binder.getCallingUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {

            val editPendingIntent = MediaStore.createWriteRequest(
                contentResolver,
                list
            )
            currentRequestCode = WRITE_REQUEST_CODE
            var request: IntentSenderRequest =
                IntentSenderRequest.Builder(editPendingIntent.intentSender).build()
            startSenderForResult.launch(request)


        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show()
        }


        */
/*  val values = ContentValues().apply {
              put(MediaStore.Images.Media.DISPLAY_NAME,"JPEG_${timeStamp}_.jpg")
              put(MediaStore.Images.Media.MIME_TYPE, "image/*")
              put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES + File.separator + "MihirDemo" )
          }
          val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
          var fos: FileOutputStream = contentResolver.openOutputStream(Objects.requireNonNull(imageUri)!!) as FileOutputStream
          image.compress(Bitmap.CompressFormat.JPEG, 80, fos)
          Objects.requireNonNull<OutputStream?>(fos)

          if (imageUri != null) {
              shareUri = imageUri
          }
*//*


         *//*


    }

}
else if (uri.toString().startsWith("file://")) {
    displayName = File(uri.toString()).absolutePath
    var file: File = File(displayName)
    val out = FileOutputStream(file)
    image.compress(Bitmap.CompressFormat.JPEG, 90, out)
    out.flush()
    out.close()
}*/


 */



/*

   will throw security exception

          ACTION_OPEN_DOCUMENT only requires below line of code


                   val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    contentResolver.takePersistableUriPermission(uri, takeFlags)


   contentResolver.openOutputStream(uri, "wt").use {
        val stream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        if (it != null) {
            it.write(byteArray)
        }
    }
*/


/*
catch (securityException: SecurityException) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        var recoverableSecurityException: RecoverableSecurityException? = null

        if (securityException is RecoverableSecurityException) {
            recoverableSecurityException = securityException

            val intentSender =
                recoverableSecurityException.userAction.actionIntent.intentSender

            var request: IntentSenderRequest =
                IntentSenderRequest.Builder(intentSender).build()
            startSenderForResult.launch(request)

        } else {

            Toast.makeText(this, "Created a new file with compression.", Toast.LENGTH_LONG).show()
            //throw RuntimeException(securityException.message, securityException)
        }


*/
/*
                intentSender?.let {
                    startIntentSenderForResult(intentSender, image-request-code,
                        null, 0, 0, 0, null)
                }
*//*

    } else {
        Toast.makeText(this, "Created a new file with compression.", Toast.LENGTH_LONG)
            .show()
        //throw RuntimeException(securityException.message, securityException)
    }

}
*/

