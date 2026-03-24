package com.example.medai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.medai.databinding.ActivityMainBinding
import com.example.medai.voice.ConversationManager
import com.example.medai.voice.ConversationState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationManager: ConversationManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startConversation()
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationManager = ConversationManager(this)

        setupUI()
        checkPermissionsAndStart()
        observeState()
    }

    private fun setupUI() {
        binding.btnToggle.setOnClickListener {
            when (conversationManager.state.value) {
                ConversationState.IDLE, ConversationState.ERROR -> {
                    if (conversationManager.state.value == ConversationState.ERROR) {
                        conversationManager.startListening()
                    } else {
                        checkPermissionsAndStart()
                    }
                }
                ConversationState.LISTENING -> conversationManager.stopListening()
                ConversationState.PROCESSING, ConversationState.SPEAKING -> conversationManager.stopConversation()
            }
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun checkPermissionsAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startConversation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startConversation() {
        conversationManager.initialize()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                conversationManager.isReady.collect { isReady ->
                    if (isReady) {
                        conversationManager.startListening()
                    }
                }
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    conversationManager.state.collect { state ->
                        updateUI(state)
                    }
                }

                launch {
                    conversationManager.transcript.collect { transcript ->
                        binding.tvUserText.text = transcript ?: ""
                    }
                }

                launch {
                    conversationManager.response.collect { response ->
                        binding.tvAssistantText.text = response ?: ""
                    }
                }

                launch {
                    conversationManager.error.collect { error ->
                        error?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(state: ConversationState) {
        binding.apply {
            when (state) {
                ConversationState.IDLE -> {
                    btnToggle.text = "Start"
                    ivStatus.setImageResource(android.R.drawable.presence_away)
                    progressBar.visibility = View.GONE
                }
                ConversationState.LISTENING -> {
                    btnToggle.text = "Stop Listening"
                    ivStatus.setImageResource(android.R.drawable.presence_online)
                    progressBar.visibility = View.GONE
                }
                ConversationState.PROCESSING -> {
                    btnToggle.text = "Stop"
                    ivStatus.setImageResource(android.R.drawable.presence_away)
                    progressBar.visibility = View.VISIBLE
                }
                ConversationState.SPEAKING -> {
                    btnToggle.text = "Stop"
                    ivStatus.setImageResource(android.R.drawable.presence_away)
                    progressBar.visibility = View.GONE
                }
                ConversationState.ERROR -> {
                    btnToggle.text = "Retry"
                    ivStatus.setImageResource(android.R.drawable.presence_offline)
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val editTextUrl = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOllamaUrl)
        val editTextModel = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etModelName)

        editTextUrl.setText(conversationManager.ollamaBaseUrl)
        editTextModel.setText(conversationManager.modelName)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                conversationManager.ollamaBaseUrl = editTextUrl.text.toString()
                conversationManager.modelName = editTextModel.text.toString()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        conversationManager.destroy()
    }
}
