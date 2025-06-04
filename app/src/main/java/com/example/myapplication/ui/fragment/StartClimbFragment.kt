package com.example.myapplication.ui.fragment

import android.content.ContentValues
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.FirstActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentStartClimbBinding
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Date
import java.util.Locale


class StartClimbFragment : Fragment() {

    private var _binding: FragmentStartClimbBinding? = null
    private val binding get() = _binding!!
    private var pictureUri: Uri? = null
    private lateinit var tempPicture: File

    private lateinit var chronometer: Chronometer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentStartClimbBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navController = findNavController()

        chronometer = binding.timer

        if (!FirstActivity.isLoggedIn()) {
            val dialog = CheckAuthenticationDialogFragment()
            dialog.show(
                childFragmentManager,
                CheckAuthenticationDialogFragment.TAG
            )
            dialog.setNavController(navController)
        }
        else{
            var chronometerStatus = false
            var stoppedTime: Long = 0
            var chronometerBase: Long = SystemClock.elapsedRealtime()

            var cacheFile = File(requireContext().cacheDir, "cache")
            if (cacheFile.exists()) {
                FileInputStream(cacheFile).use { fis ->
                    DataInputStream(BufferedInputStream(fis)).use { dis ->
                        chronometerStatus = dis.readBoolean()
                        stoppedTime = dis.readLong()
                        chronometerBase = dis.readLong()
                    }
                }
            } else {
                cacheFile = File.createTempFile("cache", null, requireContext().cacheDir)
            }

            val startButton = binding.startButton
            val stopButton = binding.stopButton

            if (chronometerStatus) {
                chronometer.base = chronometerBase
                startButton.text = "Pause"
                chronometer.start()
            } else {
                chronometer.base = SystemClock.elapsedRealtime() - stoppedTime
                chronometerBase = chronometer.base
            }

            startButton.setOnClickListener {
                if (!chronometerStatus) {
                    chronometer.start()
                    startButton.text = "Pause"
                    chronometer.base = SystemClock.elapsedRealtime() - stoppedTime
                    chronometerBase = chronometer.base
                    chronometerStatus = true
                    saveOnCache(true, stoppedTime, chronometerBase)
                } else {
                    chronometer.stop()
                    startButton.text = "Start"
                    stoppedTime = SystemClock.elapsedRealtime() - chronometer.base
                    chronometerStatus = false
                    saveOnCache(false, stoppedTime, chronometerBase)
                }
            }

            stopButton.setOnClickListener {
                if (chronometerStatus)
                    stoppedTime = SystemClock.elapsedRealtime() - chronometer.base
                chronometer.stop()
                val args = Bundle().apply { putLong("timeBase", stoppedTime) }
                navController.navigate(R.id.navigation_save_mountain, args)
                cacheFile.delete()
            }

            val pictureIcon = binding.pictureIcon
            pictureUri = getTmpFileUri()

            pictureIcon.setOnClickListener {
                takePictureLauncher.launch(pictureUri)
            }

            val thumb = binding.galleryThumb
            thumb.setImageURI(getLastImage())
            thumb.setOnClickListener {
                navController.navigate(R.id.navigation_gallery)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        chronometer.stop()
    }

    fun saveOnCache(status: Boolean, stoppedTime: Long, chronometerBase: Long) {
        val cacheFile = File(requireContext().cacheDir, "cache")
        FileOutputStream(cacheFile).use { fos ->
            DataOutputStream(BufferedOutputStream(fos)).use { dos ->
                dos.writeBoolean(status)
                dos.writeLong(stoppedTime)
                dos.writeLong(chronometerBase)
            }
        }
    }

    val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val newImage = saveImageToGallery(pictureUri!!, "SafeClimb_${timeStamp}")
            tempPicture.delete()
            val thumb = binding.galleryThumb
            thumb.setImageURI(newImage)
        }
    }

    private fun getTmpFileUri(): Uri {
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        tempPicture = File.createTempFile(
            "Picture",
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            requireContext(),
            "com.example.myapplication.fileprovider",
            tempPicture
        )
    }

    fun saveImageToGallery(sourceImageUri: Uri, displayName: String): Uri? {
        val contentResolver = requireContext().contentResolver

        val imageCollection: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "SafeClimb")
        }

        var galleryImageUri: Uri? = null
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null

        try {
            galleryImageUri = contentResolver.insert(imageCollection, contentValues)
            if (galleryImageUri == null) {
                return null
            }

            outputStream = contentResolver.openOutputStream(galleryImageUri)
            inputStream = contentResolver.openInputStream(sourceImageUri)

            if (outputStream != null && inputStream != null) {
                inputStream.copyTo(outputStream)
            }
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }

        return galleryImageUri
    }

    fun getLastImage(): Uri? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("SafeClimb")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val queryUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            requireContext().contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri: Uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    return contentUri
                }
            }
        }
        catch (e: Exception) {
        }
        return null
    }

}