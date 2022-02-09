package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderModel

@Dao
abstract class OrdersDao {
    @Query("SELECT * FROM OrderEntity")
    abstract suspend fun getAllOrders(): List<WCOrderModel>

    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateOrder(order: WCOrderModel)

    @Query("SELECT * FROM OrderEntity WHERE orderId = :orderId AND localSiteId = :localSiteId")
    abstract suspend fun getOrder(orderId: Long, localSiteId: LocalId): WCOrderModel?

    @Query("SELECT * FROM OrderEntity WHERE orderId = :orderId AND localSiteId = :localSiteId")
    abstract fun observeOrder(orderId: Long, localSiteId: LocalId): Flow<WCOrderModel?>

    @Transaction
    open suspend fun updateLocalOrder(
        orderId: Long,
        localSiteId: LocalId,
        updateOrder: WCOrderModel.() -> WCOrderModel
    ) {
        getOrder(orderId, localSiteId)
            ?.let(updateOrder)
            ?.let { insertOrUpdateOrder(it) }
    }

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId AND status IN (:status)")
    abstract suspend fun getOrdersForSite(localSiteId: LocalId, status: List<String>): List<WCOrderModel>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun getOrdersForSite(localSiteId: LocalId): List<WCOrderModel>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract fun observeOrdersForSite(localSiteId: LocalId): Flow<List<WCOrderModel>>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId AND status IN (:status)")
    abstract fun observeOrdersForSite(localSiteId: LocalId, status: List<String>): Flow<List<WCOrderModel>>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId AND orderId IN (:orderIds)")
    abstract fun getOrdersForSiteByRemoteIds(
        localSiteId: LocalId,
        orderIds: List<Long>
    ): List<WCOrderModel>

    @Query("DELETE FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract fun deleteOrdersForSite(localSiteId: LocalId)

    @Query("SELECT COUNT(*) FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract fun getOrderCountForSite(localSiteId: LocalId): Int

    @Query("DELETE FROM OrderEntity WHERE localSiteId = :localSiteId AND remoteOrderId = :orderId")
    abstract suspend fun deleteOrder(localSiteId: LocalId, orderId: Long)
}
