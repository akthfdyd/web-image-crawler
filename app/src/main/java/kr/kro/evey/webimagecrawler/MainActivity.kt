package kr.kro.evey.webimagecrawler

import android.annotation.SuppressLint
import android.app.Activity
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    val TAG = "WebImageCrawler"
    val MESSAGE_SCROLLED = 1
    val MESSAGE_DOWNLOADED = 2
    var map: HashMap<String, String> = HashMap()

    val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_SCROLLED -> {
                    downButton.performClick()
                    downDelayed()
                }
                MESSAGE_DOWNLOADED -> {
                    scrollButton.performClick()
                    scrollDelayed()
                }
            }
        }
    }

    fun downDelayed() {
        handler.sendEmptyMessageDelayed(MESSAGE_DOWNLOADED, 1000)
    }

    fun scrollDelayed() {
        handler.sendEmptyMessageDelayed(MESSAGE_SCROLLED, 1000)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PRDownloader.initialize(applicationContext);

        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.addJavascriptInterface(JSInterface(this, object : JSInterface.JSInterfaceListener {
            override fun onList(jsonStr: String) {
                Log.i(TAG, "onList() >> $jsonStr")
                val jsonArray = JSONArray(jsonStr)
                for (index in 0 until jsonArray.length()) {
                    val url: String = jsonArray.get(index).toString()
                    Log.i(TAG, "jsonItem >> $url")
                    checkImageUrl(url)
                }
            }
        }), "android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                addressEditText.setText(url)
            }
        }

        goButton.setOnClickListener {
            webView.loadUrl(addressEditText.text.toString())
        }
        autoDownButton.setOnClickListener {
            handler.sendEmptyMessage(MESSAGE_DOWNLOADED)
        }
        scrollButton.setOnClickListener {
            webView.loadUrl("javascript:window.scrollTo(0,99999999);")
        }
        downButton.setOnClickListener {
            webView.loadUrl("javascript:function img_find() { var imgs = document.getElementsByTagName(\"img\"); var imgSrcs = []; for (var i = 0; i < imgs.length; i++) { imgSrcs.push(imgs[i].src); } return imgSrcs; }")
            webView.loadUrl("javascript:window.android.getImageList(JSON.stringify(img_find()));")
        }

        webView.loadUrl("https://mobile.twitter.com")
    }

    fun checkImageUrl(url: String) {
        if (!map.containsKey(url)) {
            map.set(url, url)
            if (url.contains("small") || url.contains("360x360")) {
                var newUrl = url.replace("small", "large")
                newUrl = newUrl.replace("360x360", "large")
                var fileName = newUrl.replace("https://pbs.twimg.com/media/", "")
                fileName = fileName.replace("?format=jpg&name=large", "")
                val dirPath = getExternalFilesDir(null)?.absolutePath
                dirPath?.let { downloadFile(newUrl, it, "$fileName.jpg") }
            }
        }
    }

    fun downloadFile(url: String, dirPath: String, fileName: String) {
        Log.i(TAG, "downloadFile dirPath=$dirPath")
        val downloadId = PRDownloader.download(url, dirPath, fileName)
            .build()
            .setOnStartOrResumeListener { Log.i(TAG, "onStart $fileName") }
            .setOnPauseListener { Log.i(TAG, "onPause $fileName") }
            .setOnCancelListener { Log.i(TAG, "onCancel $fileName") }
            .setOnProgressListener { Log.i(TAG, "onProgress $fileName progress=$it") }
            .start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    Log.i(TAG, "onDownloadComplete $fileName")
                }

                override fun onError(error: Error?) {
                    Log.i(TAG, "onError $fileName ${error.toString()}")
                }
            })
    }

    internal class JSInterface(
        private val activity: Activity,
        private val listener: JSInterfaceListener
    ) {

        interface JSInterfaceListener {
            fun onList(jsonStr: String)
        }

        @JavascriptInterface
        fun getImageList(jsonStr: String) {
            Log.i(
                (activity as MainActivity).TAG, "JavascriptInterface getImageList() >> $jsonStr"
            )
            listener.onList(jsonStr)
        }
    }
}