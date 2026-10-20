package com.enol.salesforze.data.local.di

import android.content.Context
import androidx.room.Room
import com.enol.salesforze.data.local.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideDistrictDao(database: AppDatabase): DistrictDao {
        return database.districtDao()
    }

    @Provides
    @Singleton
    fun provideBeatDao(database: AppDatabase): BeatDao {
        return database.beatDao()
    }

    @Provides
    @Singleton
    fun provideClientDao(database: AppDatabase): ClientDao {
        return database.clientDao()
    }

    @Provides
    @Singleton
    fun provideMechanicDao(database: AppDatabase): MechanicDao {
        return database.mechanicDao()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    @Singleton
    fun provideOrderDao(database: AppDatabase): OrderDao {
        return database.orderDao()
    }

    @Provides
    @Singleton
    fun provideAttendanceDao(database: AppDatabase): AttendanceDao {
        return database.attendanceDao()
    }

    @Provides
    @Singleton
    fun provideVisitDao(database: AppDatabase): VisitDao {
        return database.visitDao()
    }

    @Provides
    @Singleton
    fun provideBreadcrumbDao(database: AppDatabase): BreadcrumbDao {
        return database.breadcrumbDao()
    }

    @Provides
    @Singleton
    fun provideTadaClaimDao(database: AppDatabase): TadaClaimDao {
        return database.tadaClaimDao()
    }

    @Provides
    @Singleton
    fun provideTargetKpiDao(database: AppDatabase): TargetKpiDao {
        return database.targetKpiDao()
    }

    @Provides
    @Singleton
    fun provideCompetitionDao(database: AppDatabase): CompetitionDao {
        return database.competitionDao()
    }

    @Provides
    @Singleton
    fun providePendingSyncQueueDao(database: AppDatabase): PendingSyncQueueDao {
        return database.pendingSyncQueueDao()
    }
}
