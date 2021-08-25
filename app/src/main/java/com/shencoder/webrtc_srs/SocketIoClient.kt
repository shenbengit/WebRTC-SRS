package com.shencoder.webrtc_srs

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.EngineIOException
import okhttp3.OkHttpClient
import org.json.JSONArray
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*


/**
 *
 * @author  ShenBen
 * @date    2021/8/25 10:15
 * @email   714081644@qq.com
 */
class SocketIoClient {
    private lateinit var socket: Socket

    private var callback: (String) -> Unit = {}

    fun setCallback(callback: (String?) -> Unit) {
        this.callback = callback
    }

    fun connect() {
        val xtm: X509TrustManager = object : X509TrustManager {

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }

        }

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf<TrustManager>(xtm), SecureRandom())
        val DO_NOT_VERIFY: HostnameVerifier = HostnameVerifier { hostname, session -> true }
        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory)
            .hostnameVerifier(DO_NOT_VERIFY)
            .build()
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
        IO.setDefaultOkHttpCallFactory(okHttpClient)
        socket = IO.socket("wss://192.168.2.139:8089/srs")
        socket.on(Socket.EVENT_CONNECT) {
            println("SocketIoClient-->EVENT_CONNECT--->${it}")
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            println("SocketIoClient-->EVENT_DISCONNECT--->${it}")
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            if (it.isNotEmpty()) {
                val any = it[0]
                when (any) {
                    is EngineIOException -> {
                        println("SocketIoClient-->EVENT_CONNECT_ERROR--->${any.toString()}")
                        return@on
                    }
                }
                println("SocketIoClient-->EVENT_CONNECT_ERROR--->${it[0].javaClass.simpleName}")
            } else {
                println("SocketIoClient-->EVENT_CONNECT_ERROR--->")
            }

        }.on("joined") {
            println("SocketIoClient-->joined--->")
        }.on("push_webrtc") {
            callback(it[0] as String)
            println("SocketIoClient-->push_webrtc--->${it[0]}")
        }.on("in_room_other_client") {
            val array=it[0] as JSONArray
            val string = array.getString(0)
            callback( string)
            println("SocketIoClient-->in_room_other_client--->${it[0].javaClass.simpleName}")
        }.on("test_list"){
            val it1 = it
            val jsonArray = it[0] as JSONArray
            val get1 = jsonArray.getString(0)
            val get2 = jsonArray.getString(1)
            println("test_list")
        }
        socket.connect()
    }

    fun joinRoom() {
        socket.emit("join_room", "0101")
    }

    fun pullWebRTC(url: String) {
        socket.emit("pull_webrtc", "0101", url)
    }

    fun close() {
        socket.close()
    }
}