package com.enol.salesforze.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.enol.salesforze.data.local.converters.Converters
import com.enol.salesforze.data.local.dao.AttendanceDao
import com.enol.salesforze.data.local.dao.BeatDao
import com.enol.salesforze.data.local.dao.BreadcrumbDao
import com.enol.salesforze.data.local.dao.ClientDao
import com.enol.salesforze.data.local.dao.CompetitionDao
import com.enol.salesforze.data.local.dao.DistrictDao
import com.enol.salesforze.data.local.dao.MechanicDao
import com.enol.salesforze.data.local.dao.OrderDao
import com.enol.salesforze.data.local.dao.PendingSyncQueueDao
import com.enol.salesforze.data.local.dao.ProductDao
import com.enol.salesforze.data.local.dao.TadaClaimDao
import com.enol.salesforze.data.local.dao.TargetKpiDao
import com.enol.salesforze.data.local.dao.UserDao
import com.enol.salesforze.data.local.dao.VisitDao
import com.enol.salesforze.data.local.entities.AttendanceEntity
import com.enol.salesforze.data.local.entities.BeatEntity
import com.enol.salesforze.data.local.entities.BreadcrumbEntity
import com.enol.salesforze.data.local.entities.ClientEntity
import com.enol.salesforze.data.local.entities.CompetitionEntity
import com.enol.salesforze.data.local.entities.DistrictEntity
import com.enol.salesforze.data.local.entities.LedgerEntryEntity
import com.enol.salesforze.data.local.entities.MechanicEntity
import com.enol.salesforze.data.local.entities.OrderEntity
import com.enol.salesforze.data.local.entities.OrderItemEntity
import com.enol.salesforze.data.local.entities.PendingSyncQueueEntity
import com.enol.salesforze.data.local.entities.ProductEntity
import com.enol.salesforze.data.local.entities.TadaClaimEntity
import com.enol.salesforze.data.local.entities.TargetKpiEntity
import com.enol.salesforze.data.local.entities.UserEntity
import com.enol.salesforze.data.local.entities.VisitEntity

@Database(
    entities = [
        UserEntity::class,
        DistrictEntity::class,
        BeatEntity::class,
        ClientEntity::class,
        LedgerEntryEntity::class,
        MechanicEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        AttendanceEntity::class,
        VisitEntity::class,
        BreadcrumbEntity::class,
        TadaClaimEntity::class,
        TargetKpiEntity::class,
        CompetitionEntity::class,
        PendingSyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun districtDao(): DistrictDao
    abstract fun beatDao(): BeatDao
    abstract fun clientDao(): ClientDao
    abstract fun mechanicDao(): MechanicDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun visitDao(): VisitDao
    abstract fun breadcrumbDao(): BreadcrumbDao
    abstract fun tadaClaimDao(): TadaClaimDao
    abstract fun targetKpiDao(): TargetKpiDao
    abstract fun competitionDao(): CompetitionDao
    abstract fun pendingSyncQueueDao(): PendingSyncQueueDao

    companion object {
        const val DATABASE_NAME = "enol_salesforze.db"
    }
}
