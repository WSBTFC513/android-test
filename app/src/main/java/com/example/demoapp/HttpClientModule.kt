package com.example.demoapp

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.security.KeyStore
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class GetCredentials(private var clientCertPath: String, private var privateKeyPasswordPath: String) {
    companion object {
        private const val TAG = "GetCredentials"
    }

    private var rootCaCert: String = getAmazonRootCA()

    @RequiresPermission(allOf = [Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE])
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

    @RequiresPermission(allOf = [Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE])
    @Throws(Exception::class)
    fun get(): String? {
        val okHttpClient: OkHttpClient = buildHttpClient()
        val url = ""

        // リクエストを作成
        val request = Request.Builder()
            .url(url)
            .build()

        // クライアントを使用してリクエストを実行
        val response = okHttpClient.newCall(request).execute()

        // レスポンスを処理
        if (!response.isSuccessful) {
            Log.e(TAG, "Failed to fetch data. HTTP code: ${response.code}")
        }
        val result = response.body?.string()
        println("Response: ${result}")
        response.close()
        return result
    }

    private fun buildHttpClient(): OkHttpClient {
        // クライアント証明書とプライベートキーを読み込む
        val privateKeyPassword: CharArray = File(privateKeyPasswordPath).readText().trim().toCharArray()
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(FileInputStream(clientCertPath), privateKeyPassword)
        }

        // ルートCA証明書を読み込む
        val trustStore = KeyStore.getInstance("JKS").apply {
            val caInputStream = ByteArrayInputStream(rootCaCert.toByteArray(Charsets.UTF_8))
            load(caInputStream, null)
        }

        // KeyManagerFactoryを設定
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, privateKeyPassword)
        }

        // TrustManagerFactoryを設定
        val trustManagerFactory = TrustManagerFactory.getInstance("X509").apply {
            init(trustStore)
        }

        // SSLContextを構築
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
        }

        // OkHttpClientを構築
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
            .build()

        // 上記でダメだったら(https://qiita.com/rs_tukki/items/3c7abc4181e7ac58127f)を参照して細部を変える
    }
}
