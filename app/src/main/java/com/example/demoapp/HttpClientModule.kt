package com.example.demoapp

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

class HttpClientModule {
    companion object {
        private const val TAG = "HttpClientModule"
    }

    private var rootCA: String? = null

    init {
        thread {
            rootCA = getAmazonRootCA()
            getCredentials()
            return@thread
        }
    }

    @Throws(Exception::class)
    private fun getAmazonRootCA(): String {
        val url = URL("https://www.amazontrust.com/repository/AmazonRootCA1.pem")
        val connection = url.openConnection() as HttpsURLConnection
        try {
            // 接続
            connection.connect()

            // レスポンス確認
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                // 接続エラー処理
                throw Exception("connect error")
            }

            // レスポンスのbodyの読み込み
            val inputStream: InputStream = connection.inputStream
            val bs = ByteArrayOutputStream()
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } != -1) {
                bs.write(buf, 0, len)
            }
            bs.flush()

            // 文字列化
            val bodyString = String(bs.toByteArray(), charset("UTF-8"))

            // streamを閉じる
            bs.close()
            inputStream.close()

            return bodyString
        } catch (e: Exception) {
            // エラー処理
            Log.e(TAG, e.toString())
            throw e
        }
    }

    @Throws(Exception::class)
    private fun getCredentials() {

    }
}
