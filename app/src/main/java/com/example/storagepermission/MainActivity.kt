package com.example.storagepermission

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.storagepermission.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val cameraRequest = 1888
    private val pickImage = 100
    private val writeStorageRequestCode = 101
    private var imageUri2: Uri? = null
    lateinit var currentPhotoPath: String
    private lateinit var cameraProvider: ProcessCameraProvider
    var imagePath: String = ""
    private lateinit var bitmapImage: Bitmap
    var writePermission: Boolean = false


    private var cameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            imagePath = currentPhotoPath
            binding.imgImage.setImageURI(imagePath.toUri())

            Log.e("Tag", "Launcher ---> $imagePath")

            val options = BitmapFactory.Options()
            options.inPreferredConfig =
                Bitmap.Config.ARGB_8888 // or other bitmap configuration as needed


            bitmapImage = BitmapFactory.decodeFile(imagePath, options)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        init()
        askForPermission()
    }

    private fun askForPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    writeStorageRequestCode
                )
            } else {
                dialogForStoragePermission()
            }
        } else {
            //No need to check for permission
        }
    }

    private fun dialogForStoragePermission() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Permission Required!")
        dialog.setMessage("For Store Images and Video in device we need storage permission, Click on \"ok\" button and click on allow while It'll ask for permission.")

        dialog.setPositiveButton("Ok") { _, _ ->
            askForPermission()
        }

        dialog.setNegativeButton("Cancel") { _, _ ->
        }
        dialog.show()
    }


    private fun init() {
        binding.btnSave.setOnClickListener {
            val fileName: String = binding.edtFilename.text.toString()
            val fileData: String = binding.edtFileData.text.toString()
            val fileOutputStream: FileOutputStream
            try {
                fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
                fileOutputStream.write(fileData.toByteArray())
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(applicationContext, "data save", Toast.LENGTH_LONG).show()
            binding.edtFilename.text?.clear()
            binding.edtFileData.text?.clear()
        }


        binding.btnView.setOnClickListener {
            val fileName = binding.edtFilename.text.toString()
            if (fileName.trim() != "") {
                var fileInputStream: FileInputStream? = null
                fileInputStream = openFileInput(fileName)
                val inputStreamReader = InputStreamReader(fileInputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val stringBinding: StringBuilder = StringBuilder()
                var text: String?
                while (run {
                        text = bufferedReader.readLine()
                        text
                    } != null) {
                    stringBinding.append(text)
                }
                binding.edtFileData.setText(stringBinding.toString()).toString()
            } else {
                Toast.makeText(
                    applicationContext,
                    "file name cannot be blank",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

        binding.btnCamera.setOnClickListener {
            moveToCamera()
        }

        binding.btnGallery.setOnClickListener {
            val gallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }

        binding.btnSaveToGallery.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                askForPermission()
            } else {
                try {
                    saveImageToMediaStore(contentResolver, bitmapImage)
                } catch (e: FileNotFoundException) {
                    val snack = Snackbar.make(binding.root, "Image Not found", Snackbar.LENGTH_LONG)
                    snack.show()
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: java.lang.NumberFormatException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    val snack =
                        Snackbar.make(binding.root, "Image saved Failed!", Snackbar.LENGTH_LONG)
                    snack.show()
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.e("TAG", "onRequestPermissionsResult: -------------", )
            try {
                saveImageToMediaStore(contentResolver, bitmapImage)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: java.lang.NumberFormatException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            Log.e("TAG", "absolutePath: $absolutePath")
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
        Log.e("TAG", "createImageFile: $currentPhotoPath")
        return file
    }

    private fun moveToCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            null
        }
        val photoURI: Uri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.provider",
            photoFile!!
        )
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        cameraResultLauncher.launch(takePictureIntent)
    }

    private fun saveImageToMediaStore(contentResolver: ContentResolver, bitmap: Bitmap): Uri? {

        // Check if the app has permission to write to the external storage
        val writePermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || writePermission == PackageManager.PERMISSION_GRANTED) {

            // Get the current date and time
            val currentTime = System.currentTimeMillis()
            val currentTimeString =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date(currentTime))

            // Set the display name and file name for the image
            val displayName = "My Image $currentTimeString"
            val fileName = "IMG_$currentTimeString.jpg"

            // Set the content values for the image
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, currentTime / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
            }

            // Check if the app is running on Android Q or later
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                // Set the relative path and the content values for the image
                contentValues.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES
                )
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 1)

                // Insert the content values into the MediaStore and get the URI of the image
                val collection =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val imageUri = contentResolver.insert(collection, contentValues)

                // Open an output stream for the image and write the bitmap to it
                imageUri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }

                    // Set the IS_PENDING value to 0 to indicate that the image is no longer pending
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)
                }

                // Return the URI of the image
                Log.e("TAG", "--> Return $imageUri")
                val snack =
                    Snackbar.make(
                        binding.root,
                        "Image Saved successfully",
                        Snackbar.LENGTH_LONG
                    )
                snack.show()
                return imageUri

            } else {

                // Get the directory for saving images
                val directory =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = File(directory, fileName)

                // Open an output stream for the image and write the bitmap to it
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // Set the content values for the image and insert them into the MediaStore
                contentValues.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                val imageUri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                // Return the URI of the image
                Log.e("TAG", "--> Return $imageUri")
                val snack =
                    Snackbar.make(
                        binding.root,
                        "Image Saved successfully",
                        Snackbar.LENGTH_LONG
                    )
                snack.show()
                return imageUri
            }

        } else {
            // App does not have permission to write to the external storage
            Log.e("TAG", "--> Return Null")
            val snack = Snackbar.make(
                binding.root,
                "Failed to Save!, There is some issue",
                Snackbar.LENGTH_LONG
            )
            snack.show()
            return null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e("TAG", "onActivityResult()1 --> $imageUri2 $resultCode")

        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri2 = data?.data
            binding.imgImage.setImageURI(imageUri2)
            Log.e("TAG", "onActivityResult()2 --> $imageUri2 $resultCode")
        }

        if (resultCode == RESULT_OK && requestCode == cameraRequest) {
            val photo: Bitmap? = data!!.extras!!["data"] as Bitmap?
            binding.imgImage.setImageBitmap(photo)

        }
    }
}