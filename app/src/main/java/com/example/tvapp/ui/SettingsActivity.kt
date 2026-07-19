package com.example.tvapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tvapp.data.ConfigManager
import com.example.tvapp.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLoad.setOnClickListener { loadConfig() }
        binding.btnSave.setOnClickListener { saveAndEnter() }

        renderSites()
    }

    private fun renderSites() {
        val sites = ConfigManager.config?.sites ?: emptyList()
        if (sites.isEmpty()) {
            binding.spSites.isEnabled = false
            return
        }
        binding.spSites.isEnabled = true
        binding.spSites.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            sites.map { it.name ?: it.key ?: "未知源" }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val idx = sites.indexOfFirst { it.key == ConfigManager.activeSiteKey }
        if (idx >= 0) binding.spSites.setSelection(idx)
    }

    private fun loadConfig() {
        val url = binding.etUrl.text.toString().trim()
        if (url.isEmpty()) return
        binding.tvStatus.text = getString(com.example.tvapp.R.string.status_loading)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { ConfigManager.loadFromUrl(url) }
                withContext(Dispatchers.Main) {
                    val count = ConfigManager.config?.sites?.size ?: 0
                    binding.tvStatus.text =
                        "${getString(com.example.tvapp.R.string.status_ok)}（共 $count 个源）"
                    renderSites()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text =
                        "${getString(com.example.tvapp.R.string.status_fail)}：${e.message}"
                }
            }
        }
    }

    private fun saveAndEnter() {
        val sites = ConfigManager.config?.sites
        if (sites.isNullOrEmpty()) {
            toast("请先加载配置")
            return
        }
        val pos = binding.spSites.selectedItemPosition
        ConfigManager.setActiveSite(sites.getOrNull(pos)?.key)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
