package com.enol.salesforze.data.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.*
import com.enol.salesforze.data.remote.api.EnterpriseFieldApi
import com.enol.salesforze.data.remote.session.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import java.io.IOException

@HiltWorker
class DeltaSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: EnterpriseFieldApi,
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val lastSync = sessionManager.getLastSyncTimestamp()
            val syncResponse = api.getDeltaSync(since = lastSync)
            val delta = syncResponse.delta

            database.withTransaction {
                // 1. Sync Users
                delta.users?.let { users ->
                    val userEntities = users.map { dto ->
                        UserEntity(
                            id = dto.id,
                            email = dto.email,
                            fullName = dto.fullName,
                            phone = dto.phone,
                            role = dto.role,
                            managerId = dto.managerId,
                            hierarchyPath = dto.hierarchyPath,
                            vehicleType = dto.vehicleType ?: "MOTORCYCLE",
                            vehicleNumber = dto.vehicleNumber,
                            fuelRatePerKm = dto.fuelRatePerKm ?: 7.50,
                            dailyAllowance = dto.dailyAllowance ?: 500.00,
                            isActive = dto.isActive,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.userDao().insertUsers(userEntities)
                }

                // 2. Sync Districts
                delta.districts?.let { districts ->
                    val districtEntities = districts.map { dto ->
                        DistrictEntity(
                            id = dto.id,
                            name = dto.name,
                            code = dto.code,
                            province = dto.province,
                            polygonGeoJson = dto.polygonGeoJson?.toString(),
                            assignedZsmId = dto.assignedZsmId,
                            assignedAsmId = dto.assignedAsmId,
                            isActive = dto.isActive,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.districtDao().insertDistricts(districtEntities)
                }

                // 3. Sync Beats
                delta.beats?.let { beats ->
                    val beatEntities = beats.map { dto ->
                        BeatEntity(
                            id = dto.id,
                            districtId = dto.districtId,
                            name = dto.name,
                            code = dto.code,
                            dayOfWeek = dto.dayOfWeek,
                            assignedRepId = dto.assignedRepId,
                            isActive = dto.isActive,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.beatDao().insertBeats(beatEntities)
                }

                // 4. Sync Clients
                delta.clients?.let { clients ->
                    val clientEntities = clients.map { dto ->
                        ClientEntity(
                            id = dto.id,
                            beatId = dto.beatId,
                            districtId = dto.districtId,
                            businessName = dto.businessName,
                            ownerName = dto.ownerName,
                            phone = dto.phone,
                            cnic = dto.cnic,
                            address = dto.address,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            creditLimit = dto.creditLimit,
                            outstandingBalance = dto.outstandingBalance,
                            creditHold = dto.creditHold,
                            approvalStatus = dto.approvalStatus,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.clientDao().insertClients(clientEntities)
                }

                // 5. Sync Products
                delta.products?.let { products ->
                    val productEntities = products.map { dto ->
                        val schemes = dto.activeSchemes?.map { schemeDto ->
                            com.enol.salesforze.data.local.entities.ProductScheme(
                                type = schemeDto.type,
                                buyCartons = schemeDto.buyCartons,
                                freeCartons = schemeDto.freeCartons,
                                freeUnits = schemeDto.freeUnits,
                                minCartons = schemeDto.minCartons,
                                discountPct = schemeDto.discountPct,
                                perCartonPkr = schemeDto.perCartonPkr,
                                description = schemeDto.description
                            )
                        } ?: emptyList()

                        ProductEntity(
                            id = dto.id,
                            sku = dto.sku,
                            name = dto.name,
                            description = dto.description,
                            category = dto.category,
                            viscosityGrade = dto.viscosityGrade,
                            packSizeLiters = dto.packSizeLiters,
                            unitsPerCarton = dto.unitsPerCarton,
                            tradePrice = dto.tradePrice,
                            retailPrice = dto.retailPrice,
                            activeSchemes = schemes,
                            stockQuantity = dto.stockQuantity,
                            isActive = dto.isActive,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.productDao().insertProducts(productEntities)
                }

                // 6. Sync Orders & Order Items
                delta.orders?.let { orders ->
                    for (orderDto in orders) {
                        val orderEntity = OrderEntity(
                            id = orderDto.id,
                            orderNumber = orderDto.orderNumber,
                            clientId = orderDto.clientId,
                            salesRepId = orderDto.salesRepId,
                            beatId = orderDto.beatId,
                            totalCartons = orderDto.totalCartons,
                            totalLiters = orderDto.totalLiters,
                            grossAmount = orderDto.grossAmount,
                            discountAmount = orderDto.discountAmount,
                            netAmount = orderDto.netAmount,
                            paymentTerms = orderDto.paymentTerms,
                            creditHoldFlag = orderDto.creditHoldFlag,
                            status = orderDto.status,
                            externalOmsReference = orderDto.externalOmsReference,
                            idempotencyKey = orderDto.idempotencyKey,
                            notes = orderDto.notes,
                            createdAt = orderDto.createdAt,
                            updatedAt = orderDto.updatedAt
                        )
                        database.orderDao().insertOrder(orderEntity)

                        orderDto.items?.let { items ->
                            val itemEntities = items.map { itemDto ->
                                OrderItemEntity(
                                    id = itemDto.id,
                                    orderId = itemDto.orderId,
                                    productId = itemDto.productId,
                                    cartons = itemDto.cartons,
                                    units = itemDto.units,
                                    packSizeLiters = itemDto.packSizeLiters,
                                    totalLiters = itemDto.totalLiters,
                                    unitPrice = itemDto.unitPrice,
                                    lineTotal = itemDto.lineTotal,
                                    schemeDiscount = itemDto.schemeDiscount,
                                    netLineTotal = itemDto.netLineTotal
                                )
                            }
                            database.orderDao().insertOrderItems(itemEntities)
                        }
                    }
                }

                // 7. Sync Mechanics
                delta.mechanics?.let { mechanics ->
                    val mechanicEntities = mechanics.map { dto ->
                        MechanicEntity(
                            id = dto.id,
                            clientId = dto.clientId,
                            name = dto.name,
                            phone = dto.phone,
                            cnic = dto.cnic,
                            specialty = dto.specialty,
                            walletProvider = dto.walletProvider,
                            walletAccountNumber = dto.walletAccountNumber,
                            qrCodeId = dto.qrCodeId,
                            pointsBalance = dto.pointsBalance,
                            totalScans = dto.totalScans,
                            isVerified = dto.isVerified,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.mechanicDao().insertMechanics(mechanicEntities)
                }

                // 8. Sync KPIs
                delta.kpis?.let { kpis ->
                    val kpiEntities = kpis.map { dto ->
                        TargetKpiEntity(
                            id = dto.id,
                            userId = dto.userId,
                            periodType = dto.periodType,
                            periodStart = dto.periodStart,
                            periodEnd = dto.periodEnd,
                            targetVolumeLiters = dto.targetVolumeLiters,
                            achievedVolumeLiters = dto.achievedVolumeLiters,
                            targetRevenue = dto.targetRevenue,
                            achievedRevenue = dto.achievedRevenue,
                            targetVisits = dto.targetVisits,
                            achievedVisits = dto.achievedVisits,
                            targetNewClients = dto.targetNewClients,
                            achievedNewClients = dto.achievedNewClients,
                            calculatedKpiScore = dto.calculatedKpiScore,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.targetKpiDao().insertTargetKpis(kpiEntities)
                }

                // 9. Sync Competitions
                delta.competitions?.let { comps ->
                    val compEntities = comps.map { dto ->
                        CompetitionEntity(
                            id = dto.id,
                            title = dto.title,
                            description = dto.description,
                            startDate = dto.startDate,
                            endDate = dto.endDate,
                            targetMetric = dto.targetMetric,
                            rewardDetails = dto.rewardDetails?.toString() ?: "{}",
                            isActive = dto.isActive,
                            updatedAt = dto.updatedAt
                        )
                    }
                    database.competitionDao().insertCompetitions(compEntities)
                }
            }

            // Persist the latest sync timestamp
            sessionManager.saveLastSyncTimestamp(syncResponse.syncTimestamp)
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
