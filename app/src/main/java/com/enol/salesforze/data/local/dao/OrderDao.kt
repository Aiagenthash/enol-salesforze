package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.enol.salesforze.data.local.entities.OrderEntity
import com.enol.salesforze.data.local.entities.OrderItemEntity
import com.enol.salesforze.data.local.entities.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    fun getOrderById(id: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderByIdOnce(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE order_number = :orderNumber LIMIT 1")
    suspend fun getOrderByNumber(orderNumber: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE client_id = :clientId ORDER BY created_at DESC")
    fun getOrdersByClient(clientId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE sales_rep_id = :salesRepId ORDER BY created_at DESC")
    fun getOrdersByRep(salesRepId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY created_at DESC")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE idempotency_key = :idempotencyKey LIMIT 1")
    suspend fun getOrderByIdempotencyKey(idempotencyKey: String): OrderEntity?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    fun getOrderWithItems(orderId: String): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItemsOnce(orderId: String): OrderWithItems?

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    fun getOrderItems(orderId: String): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    suspend fun getOrderItemsOnce(orderId: String): List<OrderItemEntity>

    @Query("UPDATE orders SET status = :status, updated_at = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, updatedAt: String)

    @Transaction
    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
    }

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("DELETE FROM order_items WHERE order_id = :orderId")
    suspend fun deleteOrderItems(orderId: String)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderById(id: String)

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
}
