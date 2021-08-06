package org.wordpress.android.fluxc.network.rest.wpcom.wc.pay

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentError
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentResponsePayload
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.pay.WCPaymentCreateCustomerByOrderIdResult
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PayRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchConnectionToken(site: SiteModel): WooPayload<ConnectionTokenApiResponse> {
        val url = WOOCOMMERCE.payments.connection_tokens.pathV3
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                mapOf(),
                ConnectionTokenApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun capturePayment(
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ): WCCapturePaymentResponsePayload {
        val url = WOOCOMMERCE.payments.orders.id(orderId).capture_terminal_payment.pathV3
        val params = mapOf(
                "payment_intent_id" to paymentId
        )
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                CapturePaymentApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let { data ->
                    WCCapturePaymentResponsePayload(site, paymentId, orderId, data.status)
                } ?: WCCapturePaymentResponsePayload(
                        mapToCapturePaymentError(error = null, message = "status field is null, but isError == false"),
                        site,
                        paymentId,
                        orderId
                )
            }
            is JetpackError -> {
                WCCapturePaymentResponsePayload(
                        mapToCapturePaymentError(response.error, response.error.message ?: "Unexpected error"),
                        site,
                        paymentId,
                        orderId
                )
            }
        }
    }

    suspend fun loadAccount(site: SiteModel): WooPayload<WCPaymentAccountResult> {
        val url = WOOCOMMERCE.payments.accounts.pathV3
        val params = mapOf("_fields" to ACCOUNT_REQUESTED_FIELDS)

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                WCPaymentAccountResult::class.java
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun createCustomerByOrderId(
        site: SiteModel,
        orderId: Long
    ): WooPayload<WCPaymentCreateCustomerByOrderIdResult> {
        val url = WOOCOMMERCE.payments.orders.order(orderId).create_customer.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                restClient = this,
                site = site,
                url = url,
                body = emptyMap(),
                clazz = WCPaymentCreateCustomerByOrderIdResult::class.java
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    private fun mapToCapturePaymentError(error: WPComGsonNetworkError?, message: String): WCCapturePaymentError {
        val type = when {
            error == null -> GENERIC_ERROR
            error.apiError == "wcpay_missing_order" -> MISSING_ORDER
            error.apiError == "wcpay_payment_uncapturable" -> PAYMENT_ALREADY_CAPTURED
            error.apiError == "wcpay_capture_error" -> CAPTURE_ERROR
            error.apiError == "wcpay_server_error" -> SERVER_ERROR
            error.type == GenericErrorType.TIMEOUT -> NETWORK_ERROR
            error.type == GenericErrorType.NO_CONNECTION -> NETWORK_ERROR
            error.type == GenericErrorType.NETWORK_ERROR -> NETWORK_ERROR
            else -> GENERIC_ERROR
        }
        return WCCapturePaymentError(type, message)
    }

    companion object {
        private const val ACCOUNT_REQUESTED_FIELDS: String =
                "status,has_pending_requirements,has_overdue_requirements,current_deadline,statement_descriptor," +
                        "store_currencies,country,card_present_eligible"
    }
}
