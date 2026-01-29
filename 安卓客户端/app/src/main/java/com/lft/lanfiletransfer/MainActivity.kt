package com.lft.lanfiletransfer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lft.lanfiletransfer.core.webview.LFTWebViewFragment
import com.lft.lanfiletransfer.databinding.ActivityMainBinding
import com.lft.lanfiletransfer.modules.filepicker.LFTFilePickerHandler
import com.lft.lanfiletransfer.utils.LFTLogger

/**
 * 主 Activity
 * 
 * 负责承载 WebView Fragment，处理系统事件
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
    
    private lateinit var binding: ActivityMainBinding
    
    /** WebView Fragment */
    private var webViewFragment: LFTWebViewFragment? = null
    
    /** 文件选择 Launcher */
    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        LFTFilePickerHandler.handlePickResult(result)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        LFTLogger.i(TAG, "MainActivity onCreate")
        
        // 初始化文件选择器 Launcher
        LFTFilePickerHandler.setLauncher(pickFilesLauncher)
        
        // 检查权限
        checkAndRequestPermissions()
        
        // 加载 WebView Fragment
        if (savedInstanceState == null) {
            loadWebViewFragment()
        } else {
            // 恢复 Fragment
            webViewFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? LFTWebViewFragment
        }
    }
    
    /**
     * 加载 WebView Fragment
     */
    private fun loadWebViewFragment() {
        webViewFragment = LFTWebViewFragment()
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, webViewFragment!!)
            .commit()
    }
    
    /**
     * 检查并请求权限
     */
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // 媒体权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            // 通知权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 及以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        // 位置权限（获取 WiFi SSID 需要）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults[index] == PackageManager.PERMISSION_GRANTED
                LFTLogger.d(TAG, "权限 $permission: ${if (granted) "已授权" else "被拒绝"}")
            }
        }
    }
    
    /**
     * 处理返回键
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webViewFragment?.let { fragment ->
                if (fragment.canGoBack()) {
                    fragment.goBack()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LFTLogger.i(TAG, "MainActivity onDestroy")
    }
}
