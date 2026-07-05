package `in`.mato.signzy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.paging.PagingSource

@Entity(tableName="customers")
data class CustomerEntity(
    @PrimaryKey val id:Int,
    val name:String,
    val avatar:String,
    val maskedAccount:String,
    val balance:Double,
    val currency:String,
    val dob:String,
    val nationality:String,
    val address:String,
    val contact:String,
    val ifsc:String,
    val bankName:String?,
    val branch:String?,
    val accountType:String,
    val kycStatus:String="PENDING",
    val selfiePath:String?=null,
    val cachedAt:Long=System.currentTimeMillis()
)

@Entity(tableName="ifsc_cache")
data class IfscCacheEntity(
    @PrimaryKey val ifsc:String,
    val bank:String,
    val branch:String,
    val city:String,
    val state:String,
    val micr:String?,
    val cachedAt:Long=System.currentTimeMillis()
)

@Dao
interface CustomerDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers:List<CustomerEntity>):List<Long>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    fun getAllPaging():PagingSource<Int,CustomerEntity>
    @Query("SELECT * FROM customers WHERE (name LIKE '%' || :query || '%' OR maskedAccount LIKE '%' || :query || '%') AND (:accountType = 'All' OR accountType = :accountType) AND kycStatus = :kycStatus ORDER BY id ASC")
    fun searchPaging(query:String,accountType:String,kycStatus:String):PagingSource<Int,CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id:Int):CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun customerCount():Int
    @Query("SELECT COUNT(*) FROM customers WHERE LENGTH(currency) < 3")
    suspend fun oldCurrencyFormatCount():Int

    @Query("UPDATE customers SET bankName = :bank, branch = :branch WHERE id = :id")
    suspend fun updateBankDetails(id:Int,bank:String,branch:String):Int
    @Query("UPDATE customers SET kycStatus = 'VERIFIED', selfiePath = :selfiePath, cachedAt = :timestamp WHERE id = :id")
    suspend fun markVerified(id:Int,selfiePath:String,timestamp:Long):Int

}

@Dao
interface IfscDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insert(ifsc:IfscCacheEntity):Long
    @Query("SELECT * FROM ifsc_cache WHERE ifsc = :ifsc")
    suspend fun getByIfsc(ifsc:String):IfscCacheEntity?

}


@Database(entities=[CustomerEntity::class,IfscCacheEntity::class],version=2,exportSchema=false)
abstract class AppDatabase:RoomDatabase() {
    abstract fun customerDao():CustomerDao
    abstract fun ifscDao():IfscDao
}
