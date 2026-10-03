package com.yozora.aichat.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

data class CustomApiConfig(
    val baseUrl: String = "",
    val proxyUrl: String = ""
) {
    fun validationError(): String? = runCatching {
        apiRoot()
        networkProxy()
    }.exceptionOrNull()?.message

    fun endpoint(route: String): String = apiRoot().newBuilder()
        .addPathSegments(route)
        .build().toString()

    private fun apiRoot(): okhttp3.HttpUrl {
        val url = baseUrl.trim().toHttpUrlOrNull()
        require(url != null) { "Enter a valid http:// or https:// base URL." }
        require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) {
            "Use a base URL without credentials, query parameters, or a fragment."
        }
        val path = url.encodedPath.trimEnd('/')
            .removeSuffix("/chat/completions")
            .removeSuffix("/models")
            .ifBlank { "/v1" }
        return url.newBuilder().encodedPath("$path/").build()
    }

    internal fun networkProxy(): Proxy? {
        if (proxyUrl.isBlank()) return null
        val uri = runCatching { URI(proxyUrl.trim()) }.getOrNull()
        require(uri != null && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty()) {
            "Use a proxy URL such as socks5://127.0.0.1:1080 or http://proxy.example.com:8080."
        }
        val type = when (uri.scheme?.lowercase()) {
            "socks5", "socks" -> Proxy.Type.SOCKS
            "http" -> Proxy.Type.HTTP
            else -> error("Proxy must use http:// or socks5://.")
        }
        val port = if (uri.port == -1) {
            if (type == Proxy.Type.SOCKS) 1080 else 8080
        } else uri.port
        require(port in 1..65535) { "Proxy port must be between 1 and 65535." }
        return Proxy(type, InetSocketAddress.createUnresolved(uri.host, port))
    }

    fun toJson(): JSONObject = JSONObject()
        .put("baseUrl", baseUrl)
        .put("proxyUrl", proxyUrl)

    companion object {
        const val GOOGLE_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"

        fun fromJson(json: JSONObject?): CustomApiConfig = CustomApiConfig(
            baseUrl = json?.optString("baseUrl").orEmpty(),
            proxyUrl = json?.optString("proxyUrl").orEmpty()
        )
    }
}
