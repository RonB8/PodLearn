package com.example.podlingo.testutil

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout

/** In-memory [Call.Factory] so repository tests can run without real sockets or MockWebServer. */
class FakeCallFactory(private val respond: (Request) -> Response) : Call.Factory {
    override fun newCall(request: Request): Call = FakeCall(request, respond)
}

private class FakeCall(private val request: Request, private val respond: (Request) -> Response) : Call {
    private var canceled = false

    override fun execute(): Response = respond(request)
    override fun enqueue(responseCallback: Callback) = throw UnsupportedOperationException("not used")
    override fun cancel() { canceled = true }
    override fun isExecuted(): Boolean = false
    override fun isCanceled(): Boolean = canceled
    override fun request(): Request = request
    override fun timeout(): Timeout = Timeout.NONE
    override fun clone(): Call = FakeCall(request, respond)
}

fun okXmlResponse(request: Request, xml: String): Response = Response.Builder()
    .request(request)
    .protocol(Protocol.HTTP_1_1)
    .code(200)
    .message("OK")
    .body(xml.toResponseBody("application/xml".toMediaType()))
    .build()
