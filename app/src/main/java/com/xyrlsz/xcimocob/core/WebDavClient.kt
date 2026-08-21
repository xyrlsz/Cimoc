package com.xyrlsz.xcimocob.core

import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.dav4jvm.ktor.PreemptiveBasicDigestAuthProvider
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.ktor.responses
import at.bitfire.dav4jvm.ktor.responsesWithRelation
import at.bitfire.dav4jvm.property.webdav.GetContentLength
import at.bitfire.dav4jvm.property.webdav.GetLastModified
import at.bitfire.dav4jvm.property.webdav.ResourceType
import at.bitfire.dav4jvm.property.webdav.WebDAV
import io.ktor.client.HttpClient
import io.ktor.client.content.LocalFileContent
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.InputStream

/**
 * 简化版 WebDAV 资源信息。
 *
 * @param name          资源名（URL 最后一段路径，目录名不带末尾斜杠）
 * @param isDirectory   是否为集合目录
 * @param contentLength 文件大小（目录为 0）
 * @param url           资源的完整 URL
 * @param lastModified  服务器返回的修改时间（epoch 毫秒，未知时为 0）
 */
data class DavResourceInfo(
    val name: String,
    val isDirectory: Boolean,
    val contentLength: Long,
    val url: String,
    val lastModified: Long
)

/**
 * 基于 dav4jvm 的阻塞式 WebDAV 客户端封装。
 *
 * 内部使用 [runBlocking] 桥接 dav4jvm 的 suspend/Flow API，
 * 所有方法都会阻塞当前调用线程，因此**必须在后台线程**中调用
 * （如 RxJava 的 [io.reactivex.rxjava3.schedulers.Schedulers.io]）。
 */
class WebDavClient private constructor(
    private val httpClient: HttpClient
) {

    /** 资源是否存在（HEAD 请求）。网络/认证错误一律视为不存在。 */
    fun exists(url: String): Boolean = runBlocking {
        try {
            DavResource(httpClient, Url(url)).head { }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** 创建集合目录（MKCOL，mkdir -p 语义：逐级创建缺失的父目录）。 */
    fun createDirectory(url: String) {
        if (exists(url)) {
            return
        }
        // 从根路径逐级检查/创建，避免父目录缺失导致 409 Conflict
        val u = Url(url)
        val segments = u.encodedPath.trim('/').split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) {
            return
        }
        val portStr = if (u.port != u.protocol.defaultPort) ":" + u.port else ""
        var current = u.protocol.name + "://" + u.host + portStr
        for (segment in segments) {
            current = "$current/$segment"
            if (exists(current)) {
                continue
            }
            runBlocking {
                DavResource(httpClient, Url(current)).mkCol(null) { }
            }
        }
    }

    /** 列出集合的直接成员（PROPFIND depth 1，不含集合自身）。 */
    fun listChildren(url: String): List<DavResourceInfo> = runBlocking {
        try {
            DavResource(httpClient, Url(url))
                .propfind(1, WebDAV.ResourceType, WebDAV.GetContentLength, WebDAV.GetLastModified)
                .responsesWithRelation()
                .toList()
                .filter { it.relation == Response.HrefRelation.MEMBER }
                .map { it.response.toResourceInfo() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 获取资源自身信息（PROPFIND depth 0）。不存在或出错返回 null。 */
    fun getResource(url: String): DavResourceInfo? = runBlocking {
        try {
            DavResource(httpClient, Url(url))
                .propfind(0, WebDAV.ResourceType, WebDAV.GetContentLength, WebDAV.GetLastModified)
                .responses()
                .firstOrNull()
                ?.toResourceInfo()
        } catch (_: Exception) {
            null
        }
    }

    /** 上传字节内容（PUT）。 */
    fun put(url: String, bytes: ByteArray) = runBlocking {
        DavResource(httpClient, Url(url)).put(ByteArrayContent(bytes)) { }
    }

    /** 上传文件（PUT，流式传输）。 */
    fun put(url: String, file: File, mimeType: String) = runBlocking {
        val contentType = try {
            ContentType.parse(mimeType)
        } catch (_: Exception) {
            ContentType.Application.OctetStream
        }
        DavResource(httpClient, Url(url)).put(
            LocalFileContent(file, contentType)
        ) { }
    }

    /** 下载文件流（GET）。返回的流在读取完成后必须关闭。 */
    fun get(url: String): InputStream = runBlocking {
        DavResource(httpClient, Url(url)).get { response ->
            response.bodyAsChannel().toInputStream()
        }
    }

    /** 删除资源（DELETE）。 */
    fun delete(url: String) = runBlocking {
        DavResource(httpClient, Url(url)).delete { }
    }

    /** 移动资源（MOVE，允许覆盖目标）。 */
    fun move(from: String, to: String) = runBlocking {
        DavResource(httpClient, Url(from)).move(Url(to), overwrite = true) { }
    }

    /** 关闭底层 HTTP 客户端。 */
    fun close() {
        httpClient.close()
    }

    private fun Response.toResourceInfo(): DavResourceInfo {
        val type = this[ResourceType::class.java]
        return DavResourceInfo(
            name = hrefName(),
            isDirectory = type?.types?.contains(WebDAV.Collection) == true,
            contentLength = this[GetContentLength::class.java]?.contentLength ?: 0L,
            url = href.toString(),
            lastModified = this[GetLastModified::class.java]?.lastModified?.toEpochMilli() ?: 0L
        )
    }

    companion object {

        /**
         * 创建 WebDAV 客户端（Basic 预认证，服务器要求 Digest 时自动切换）。
         *
         * 说明：
         * - dav4jvm 自行处理重定向，因此必须关闭 Ktor 的自动重定向；
         * - dav4jvm 自行校验 HTTP 状态并抛出其异常体系，因此关闭 expectSuccess；
         * - 大文件上传/下载禁用请求与 socket 超时（connect 超时保留 30s）。
         */
        @JvmStatic
        fun create(username: String, password: String): WebDavClient {
            val client = HttpClient(OkHttp) {
                followRedirects = false
                expectSuccess = false
                install(HttpTimeout) {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                }
                install(Auth) {
                    providers += PreemptiveBasicDigestAuthProvider(username, password)
                }
            }
            return WebDavClient(client)
        }
    }
}
