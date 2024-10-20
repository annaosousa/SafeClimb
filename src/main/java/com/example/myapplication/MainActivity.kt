package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        val buttonNavigate = findViewById<Button>(R.id.button_navigate)

        buttonNavigate.setOnClickListener {
            val intent = Intent(
                this@MainActivity,
                FirstActivity::class.java
            )
            startActivity(intent)
        }
    }
}
