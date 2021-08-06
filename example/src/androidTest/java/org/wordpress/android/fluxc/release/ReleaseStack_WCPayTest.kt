package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.example.test.BuildConfig
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult.WCPayAccountStatusEnum
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Inject

class ReleaseStack_WCPayTest : ReleaseStack_WCBase() {
    @Inject internal lateinit var payStore: WCPayStore

    override val testSite: TestSite = TestSite.Specified(siteId = BuildConfig.TEST_WPCOM_SITE_ID_WOO_JP_WCPAY)

    override fun buildAuthenticatePayload() = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_WOO_JP_WCPAY,
            BuildConfig.TEST_WPCOM_PASSWORD_WOO_JP_WCPAY
    )

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
    }

    @Test
    fun givenSiteHasWCPayWhenFetchConnectionTokenInvokedThenTokenReturned() = runBlocking {
        val result = payStore.fetchConnectionToken(sSite)

        assertTrue(result.model?.token?.isNotEmpty() == true)
    }

    @Test
    fun givenSiteHasWCPayWhenLoadAccountThenTestAccountReturned() = runBlocking {
        val result = payStore.loadAccount(sSite)

        assertEquals("US", result.model?.country)
        assertEquals(false, result.model?.hasPendingRequirements)
        assertEquals(false, result.model?.hasOverdueRequirements)
        assertEquals("DO.WPMT.CO", result.model?.statementDescriptor)
        assertEquals("US", result.model?.country)
        assertEquals(false, result.model?.isCardPresentEligible)
        assertEquals("usd", result.model?.storeCurrencies?.default)
        assertEquals(listOf("usd"), result.model?.storeCurrencies?.supportedCurrencies)
        assertEquals(WCPayAccountStatusEnum.COMPLETE, result.model?.status)
    }

    @Test
    fun givenSiteHasWCPayAndOrderWhenCreateCustomerByOrderIdCustomerIdReturned() = runBlocking {
        val result = payStore.createCustomerByOrderId(
                sSite,
                17L
        )

        assertEquals("cus_JyzaCUE61Qmy8y", result.model?.customerId)
    }

    @Test
    fun givenSiteHasWCPayAndWrongOrderIdWhenCreateCustomerByOrderIdCustomerIdReturned() = runBlocking {
        val result = payStore.createCustomerByOrderId(
                sSite,
                1L
        )

        assertTrue(result.isError)
    }
}
