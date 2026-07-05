package `in`.mato.signzy.domain.repository

import androidx.paging.PagingData
import `in`.mato.signzy.domain.model.Customer
import `in`.mato.signzy.domain.model.BankDetails
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {

    fun getCustomerList(query:String,accountType:String,kycStatus:String):Flow<PagingData<Customer>>
    fun getCustomerDetails(id:Int):Flow<Customer?>
    suspend fun refreshCustomers()
    suspend fun getBankDetails(ifsc:String):BankDetails?
    suspend fun saveSelfie(id:Int,selfiePath:String)

    fun isDarkTheme():Flow<Boolean>
    suspend fun saveTheme(isDark:Boolean)
}
