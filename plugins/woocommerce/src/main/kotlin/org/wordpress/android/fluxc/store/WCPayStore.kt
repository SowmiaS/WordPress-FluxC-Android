package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.WCCapturePaymentResponsePayload
import org.wordpress.android.fluxc.model.payments.WCConnectionTokenResult
import org.wordpress.android.fluxc.model.payments.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.WCPaymentCreateCustomerByOrderIdResult
import org.wordpress.android.fluxc.model.payments.WCTerminalStoreLocationResult
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCPayStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: PayRestClient
) {
    suspend fun fetchConnectionToken(site: SiteModel): WooResult<WCConnectionTokenResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchConnectionToken") {
            val response = restClient.fetchConnectionToken(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    WooResult(WCConnectionTokenResult(response.result.token, response.result.isTestMode))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun capturePayment(site: SiteModel, paymentId: String, orderId: Long): WCCapturePaymentResponsePayload {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "capturePayment") {
            restClient.capturePayment(site, paymentId, orderId)
        }
    }

    suspend fun loadAccount(site: SiteModel): WooResult<WCPaymentAccountResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "loadAccount") {
            restClient.loadAccount(site).asWooResult()
        }
    }

    suspend fun createCustomerByOrderId(
        site: SiteModel,
        orderId: Long
    ): WooResult<WCPaymentCreateCustomerByOrderIdResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createCustomerByOrderId") {
            restClient.createCustomerByOrderId(site, orderId).asWooResult()
        }
    }

    suspend fun getStoreLocationForSite(site: SiteModel): WCTerminalStoreLocationResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "getStoreLocationForSite") {
            restClient.getStoreLocationForSite(site)
        }
    }
}
