package org.wordpress.android.fluxc.mocked

import android.content.Context
import com.android.volley.RequestQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTimeoutRequestHandler
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_JetpackTunnelTest : MockedStack_Base() {
    companion object {
        private const val DUMMY_SITE_ID = 567L
    }

    @Inject internal lateinit var jetpackTunnelClient: JetpackTunnelClientForTests

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun testErrorResponse() {
        val countDownLatch = CountDownLatch(1)
        val url = "/"

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, DUMMY_SITE_ID, mapOf(),
                RootWPAPIRestResponse::class.java,
                { _: RootWPAPIRestResponse? ->
                    throw AssertionError("Unexpected success!")
                },
                WPComErrorListener { error ->
                    // Verify that the error response is correctly parsed
                    assertEquals("rest_no_route", error.apiError)
                    assertEquals("No route was found matching the URL and request method", error.message)
                    countDownLatch.countDown()
                },
                {})

        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        jetpackTunnelClient.exposedAdd(request)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
    }

    @Test
    fun testSuccessfulGetRequest() {
        val countDownLatch = CountDownLatch(1)
        val url = "/"
        val params = mapOf("context" to "view")

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, DUMMY_SITE_ID, params,
                RootWPAPIRestResponse::class.java,
                { response: RootWPAPIRestResponse? ->
                    // Verify that the successful response is correctly parsed
                    assertTrue(response?.namespaces?.contains("wp/v2")!!)
                    countDownLatch.countDown()
                },
                WPComErrorListener { error ->
                    throw AssertionError("Unexpected BaseNetworkError: " +
                            error.apiError + " - " + error.message)
                },
                {})

        interceptor.respondWith("jetpack-tunnel-root-response-success.json")
        jetpackTunnelClient.exposedAdd(request)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
    }

    @Test
    fun testSuccessfulPostRequest() {
        val countDownLatch = CountDownLatch(1)
        val url = "/wp/v2/settings/"

        val requestBody = mapOf<String, Any>("title" to "New Title", "description" to "New Description")

        val request = JetpackTunnelGsonRequest.buildPostRequest(url, DUMMY_SITE_ID, requestBody,
                SettingsAPIResponse::class.java,
                { response: SettingsAPIResponse? ->
                    // Verify that the successful response is correctly parsed
                    assertEquals("New Title", response?.title)
                    assertEquals("New Description", response?.description)
                    assertNull(response?.language)
                    countDownLatch.countDown()
                },
                WPComErrorListener { error ->
                    throw AssertionError("Unexpected BaseNetworkError: " +
                            error.apiError + " - " + error.message)
                })

        interceptor.respondWith("jetpack-tunnel-wp-v2-settings-response-success.json")
        jetpackTunnelClient.exposedAdd(request)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
    }

    @Test
    fun testJetpackTimeoutErrorResponseContinuous() {
        // Simulate continuously receiving Jetpack timeout error from the server
        // Expect to make the maximum number of retry attempts, and then finally receive the timeout as a normal error
        val countDownLatch = CountDownLatch(JetpackTimeoutRequestHandler.DEFAULT_MAX_RETRIES + 1)
        val url = "/"
        var retriesAttempted = 0

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, DUMMY_SITE_ID, mapOf(),
                RootWPAPIRestResponse::class.java,
                { _: RootWPAPIRestResponse? ->
                    throw AssertionError("Unexpected success!")
                },
                WPComErrorListener { error ->
                    // Verify that the error response is correctly parsed once the retry limit is exceeded
                    assertEquals("http_request_failed", error.apiError)
                    assertEquals(
                            "cURL error 28: Operation timed out after 5001 milliseconds with 11111 bytes received",
                            error.message)
                    countDownLatch.countDown()
                },
                { retryRequest ->
                    // Force the interceptor to continue responding with a Jetpack timeout error
                    retriesAttempted++
                    interceptor.respondWithError("jetpack-tunnel-timeout-error.json")
                    jetpackTunnelClient.exposedAdd(retryRequest)
                    countDownLatch.countDown()
                })

        // Start with a timeout error, to trigger the timeout retry listener of the request
        interceptor.respondWithError("jetpack-tunnel-timeout-error.json")
        jetpackTunnelClient.exposedAdd(request)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(JetpackTimeoutRequestHandler.DEFAULT_MAX_RETRIES, retriesAttempted)
    }

    @Test
    fun testJetpackTimeoutErrorResponseSuccessfulRetry() {
        // Simulate a single Jetpack timeout error from the server, then a success on retry
        // Expect to make only one retry attempt, and then receive a successful response
        val countDownLatch = CountDownLatch(2)
        val url = "/"
        var retriesAttempted = 0

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, DUMMY_SITE_ID, mapOf(),
                RootWPAPIRestResponse::class.java,
                { response: RootWPAPIRestResponse? ->
                    // Verify that the successful response is correctly parsed
                    assertTrue(response?.namespaces?.contains("wp/v2")!!)
                    countDownLatch.countDown()
                },
                WPComErrorListener { error ->
                    throw AssertionError("Unexpected BaseNetworkError: " +
                            error.apiError + " - " + error.message)
                },
                { retryRequest ->
                    // Respond with success after one retry
                    retriesAttempted++
                    interceptor.respondWith("jetpack-tunnel-root-response-success.json")
                    jetpackTunnelClient.exposedAdd(retryRequest)
                    countDownLatch.countDown()
                })

        // Start with a timeout error, to trigger the timeout retry listener of the request
        interceptor.respondWithError("jetpack-tunnel-timeout-error.json")
        jetpackTunnelClient.exposedAdd(request)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(1, retriesAttempted)
    }

    @Singleton
    class JetpackTunnelClientForTests @Inject constructor(
        appContext: Context,
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        accessToken: AccessToken,
        userAgent: UserAgent
    ) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
        /**
         * Wraps and exposes the protected [add] method so that tests can add requests directly.
         */
        fun <T> exposedAdd(request: WPComGsonRequest<T>?) { add(request) }
    }

    class SettingsAPIResponse : Response {
        val title: String? = null
        val description: String? = null
        val language: String? = null
    }
}
