package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeMapper
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.deleteSingleStoredAttribute
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.getCurrentAttributes
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.insertFromScratchCompleteAttributesList
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.insertOrUpdateSingleAttribute
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.insertSingleAttribute
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.updateSingleStoredAttribute
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCGlobalAttributeStore @Inject constructor(
    private val restClient: ProductAttributeRestClient,
    private val mapper: WCGlobalAttributeMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchStoreAttributes(
        site: SiteModel
    ): WooResult<List<WCGlobalAttributeModel>> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchStoreAttributes") {
                restClient.fetchProductFullAttributesList(site)
                        .asWooResult()
                        .model
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { mapper.responseToAttributeModelList(it, site) }
                        ?.let {
                            insertFromScratchCompleteAttributesList(it, site.id)
                            getCurrentAttributes(site.id)
                        }
                        ?.let { WooResult(it) }
                        ?: getCurrentAttributes(site.id)
                                .takeIf { it.isNotEmpty() }
                                ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun fetchAttribute(
        site: SiteModel,
        attributeID: Long
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createStoreAttributes") {
                restClient.fetchSingleAttribute(site, attributeID)
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { insertOrUpdateSingleAttribute(it, site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun createAttribute(
        site: SiteModel,
        name: String,
        slug: String = name,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createStoreAttributes") {
                restClient.postNewAttribute(
                        site, mapOf(
                        "name" to name,
                        "slug" to slug,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString()
                )
                )
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { insertSingleAttribute(it) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun updateAttribute(
        site: SiteModel,
        attributeID: Long,
        name: String,
        slug: String = name,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "updateStoreAttributes") {
                restClient.updateExistingAttribute(
                        site, attributeID, mapOf(
                        "id" to attributeID.toString(),
                        "name" to name,
                        "slug" to slug,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString()
                )
                )
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { updateSingleStoredAttribute(it, site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun deleteAttribute(
        site: SiteModel,
        attributeID: Long
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteStoreAttributes") {
                restClient.deleteExistingAttribute(site, attributeID)
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { deleteSingleStoredAttribute(it, site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun createOptionValueForAttribute(
        site: SiteModel,
        attributeID: Long,
        term: String
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createAttributeTerm") {
                restClient.postNewTerm(
                        site, attributeID,
                        mapOf("name" to term)
                )
                        .asWooResult()
                        .model
                        ?.let { fetchAttribute(site, attributeID) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun deleteOptionValueFromAttribute(
        site: SiteModel,
        attributeID: Long,
        termID: Long
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteAttributeTerm") {
                restClient.deleteExistingTerm(site, attributeID, termID)
                        .asWooResult()
                        .model
                        ?.let { fetchAttribute(site, attributeID) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
}
