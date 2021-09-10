package com.shencoder.webrtc_srs

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 *
 * @author  ShenBen
 * @date    2021/8/30 08:57
 * @email   714081644@qq.com
 */
class SignalRClient {

    private lateinit var hubConnection: HubConnection


    fun connect() {
        hubConnection = HubConnectionBuilder.create("https://192.168.2.119:5005/chat")
            .setHttpClientBuilderCallback {
                val trustManager: X509TrustManager = object : X509TrustManager {

                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }

                }

                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
                val sslSocketFactory = sslContext.socketFactory

                val hostnameVerifier = HostnameVerifier { hostname, session -> true }

                it.sslSocketFactory(sslSocketFactory, trustManager)
                    .hostnameVerifier(hostnameVerifier)
            }.build()

        hubConnection.on(
            "broadcastMessage",
            { name, message -> println("SignalRClient-broadcastMessage->name:${name},message:${message}") },
            String::class.java,
            String::class.java
        )
            .unsubscribe()
        hubConnection.start().subscribe({
            println("SignalRClient连接成功-->")
            hubConnection.invoke("send", "张三", "啦啦啦德玛西亚").subscribe({
                println("SignalRClient发送消息成功-->")
            }, {
                println("SignalRClient发送消息失败-->$it")
            })
        }, { it ->
            println("SignalRClient连接成功失败-->${it}")
        })
    }

    fun disconnect() {
        hubConnection.stop().subscribe({
            println("SignalRClient断开连接成功-->")
        }, {
            println("SignalRClient断开连接失败-->$it")
        })
    }
}