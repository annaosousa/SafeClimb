package com.example.myapplication.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.myapplication.MainActivity
import com.example.myapplication.R

class CheckAuthenticationDialogFragment : DialogFragment() {

    private var navControllerInstance: NavController? = null

    fun setNavController(navController: NavController) {
        this.navControllerInstance = navController
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage("Please authenticate to use this feature")
            .setPositiveButton("Sign In") { _,_ ->
                    val intent = Intent(
                        requireContext(),
                        MainActivity::class.java
                    )
                    intent.putExtra("screen","auth");
                requireContext().startActivity(intent)
                }
            .setNegativeButton("Cancel") { _,_ ->
                navControllerInstance?.navigate(R.id.navigation_home, null)
            }
            .create()

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.7f)
        }
        isCancelable = false
    }

    companion object {
        const val TAG = "CheckAuthenticationDialog"
    }
}