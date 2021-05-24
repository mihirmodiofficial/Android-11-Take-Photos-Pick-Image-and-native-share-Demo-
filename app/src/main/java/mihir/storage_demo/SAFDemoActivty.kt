package mihir.storage_demo

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import mihir.storage_demo.databinding.ActivityRecordNPlayBinding
import mihir.storage_demo.databinding.ActivitySAFDemoActivtyBinding
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder

class SAFDemoActivty : BaseActivity(),OnActivityResultListener {

    lateinit var binding: ActivitySAFDemoActivtyBinding

    val CREATE_TXT_FILE = 1
    val WRITE_TXT_FILE = 2
    val READ_TXT_FILE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.buttonCreate.setOnClickListener {
            createTextFile()
        }
        binding.buttonWrite.setOnClickListener {
            writeTextFile()
        }
        binding.buttonRead.setOnClickListener {
            readTextFile()
        }
        onActivityResultListener = this

    }

    private fun readTextFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        currentRequestCode = READ_TXT_FILE
        startActivityForResult.launch(intent)
    }

    private fun writeTextFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        currentRequestCode = WRITE_TXT_FILE
        startActivityForResult.launch(intent)

    }

    private fun createTextFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "mihir_test_demo.txt")
        }
        currentRequestCode = CREATE_TXT_FILE
        startActivityForResult.launch(intent)

    }

    override fun findContentView(): Int {
        return R.layout.activity_s_a_f_demo_activty
    }

    override fun bindViewWithViewBinding(view: View) {
        binding = ActivitySAFDemoActivtyBinding.bind(view)
    }

    override fun onActivityResult(result: ActivityResult, currentRequestCode: Int) {
        if(currentRequestCode  == CREATE_TXT_FILE){
            Toast.makeText(this,"File is created successfully.",Toast.LENGTH_LONG).show()
        }
        if(currentRequestCode == WRITE_TXT_FILE){
            result.data!!.data.let {
                if(it!=null){
                    writeTextFile(it)
                }
            }
        }
        if(currentRequestCode == READ_TXT_FILE){
            result.data!!.data.let {
                if(it!=null){
                    readTextFile(it)
                }
            }
        }
    }

    private fun readTextFile(uri: Uri) {
        try {
            var inputStream: InputStream = contentResolver.openInputStream(uri)!!
            var bufferedReader: BufferedReader = BufferedReader(InputStreamReader(inputStream))
            var stringBuilder: StringBuilder = StringBuilder()
            var line: String? = bufferedReader.readLine()
            while (line !=null){
                stringBuilder.append(line + "\n")
                line = bufferedReader.readLine()
            }
            inputStream.close()
            Toast.makeText(this,stringBuilder.toString(),Toast.LENGTH_LONG).show()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun writeTextFile(uri: Uri) {
        try{
            var pfd: ParcelFileDescriptor = contentResolver.openFileDescriptor(uri,"w")!!
            var fos: FileOutputStream = FileOutputStream(pfd.fileDescriptor)
            var data: String = "Test text file by Mihir Modi"
            fos.write(data.toByteArray())
            fos.close()
            pfd.close()
            Toast.makeText(this,"File written successfully.",Toast.LENGTH_LONG).show()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}