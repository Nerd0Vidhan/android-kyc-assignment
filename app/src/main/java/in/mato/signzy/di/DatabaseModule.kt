package `in`.mato.signzy.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.mato.signzy.data.local.AppDatabase
import `in`.mato.signzy.data.local.CustomerDao
import `in`.mato.signzy.data.local.IfscDao
import `in`.mato.signzy.data.local.KycDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context:Context):AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "signzy_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideCustomerDao(db:AppDatabase):CustomerDao=db.customerDao()

    @Provides
    fun provideIfscDao(db:AppDatabase):IfscDao=db.ifscDao()

    @Provides
    @Singleton
    fun provideKycDataStore(@ApplicationContext context:Context):KycDataStore {
        return KycDataStore(context)
    }
}
