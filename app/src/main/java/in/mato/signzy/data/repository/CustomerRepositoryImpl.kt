package `in`.mato.signzy.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import `in`.mato.signzy.data.local.CustomerDao
import `in`.mato.signzy.data.local.CustomerEntity
import `in`.mato.signzy.data.local.IfscCacheEntity
import `in`.mato.signzy.data.local.IfscDao
import `in`.mato.signzy.data.local.KycDataStore
import `in`.mato.signzy.data.remote.DummyJsonApi
import `in`.mato.signzy.data.remote.RazorpayIfscApi
import `in`.mato.signzy.domain.model.BankDetails
import `in`.mato.signzy.domain.model.Customer
import `in`.mato.signzy.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(
    private val customerDao:CustomerDao,
    private val ifscDao:IfscDao,
    private val dummyJsonApi:DummyJsonApi,
    private val razorpayIfscApi:RazorpayIfscApi,
    private val kycDataStore:KycDataStore
):CustomerRepository {
    
    private suspend fun syncIfEmpty() {
        if(customerDao.customerCount()==0||customerDao.oldCurrencyFormatCount()>0) {
            refreshCustomers()
        }
    }

    override suspend fun refreshCustomers() {
        try {
            val response=dummyJsonApi.getUsers(20,0)

            val ifscList=listOf("HDFC0CAGSBK","SBIN0000001","ICIC0000001","PUNB0244200","UTIB0000001")
            val types=listOf("Savings","Current","NRI")

            val entities=response.users.mapIndexed{index,user->
                val old=customerDao.getCustomerById(user.id)
                val isVerified=old?.kycStatus=="VERIFIED"||kycDataStore.isVerified(user.id).first()
                val selfiePath=old?.selfiePath?:kycDataStore.getSelfiePath(user.id).first()
                val ifsc=old?.ifsc?:ifscList[index%ifscList.size]
                val balance=old?.balance?:Math.round((1000+Math.random()*149000)*100.0)/100.0
                val type=old?.accountType?:types[index%types.size]

                CustomerEntity(
                    id=user.id,
                    name="${user.firstName} ${user.lastName}",
                    avatar=user.image,
                    maskedAccount=user.bank.iban.takeLast(4).let{"**** $it"},
                    balance=balance,
                    currency=user.bank.currency,
                    dob=user.birthDate,
                    nationality=user.address.country,
                    address="${user.address.address}, ${user.address.city}, ${user.address.state}",
                    contact=user.phone,
                    ifsc=ifsc,
                    bankName=old?.bankName,
                    branch=old?.branch,
                    accountType=type,
                    kycStatus=if(isVerified)"VERIFIED" else "PENDING",
                    selfiePath=selfiePath
                )
            }

            customerDao.insertAll(entities)

        }catch(e:Exception) {
            e.printStackTrace()
        }
    }

    override fun getCustomerList(query:String,accountType:String,kycStatus:String):Flow<PagingData<Customer>> {
        return flow {
            syncIfEmpty()
            val pager=Pager(
                config=PagingConfig(pageSize=20,enablePlaceholders=false),
                pagingSourceFactory={customerDao.searchPaging(query,accountType,kycStatus)}
            )
            pager.flow.map{pagingData->
                pagingData.map{entity->
                    Customer(
                        id=entity.id,
                        name=entity.name,
                        avatar=entity.avatar,
                        maskedAccount=entity.maskedAccount,
                        balance=entity.balance,
                        currency=entity.currency,
                        kycStatus=entity.kycStatus,
                        selfiePath=entity.selfiePath,
                        dob=entity.dob,
                        nationality=entity.nationality,
                        address=entity.address,
                        contact=entity.contact,
                        ifsc=entity.ifsc,
                        bankName=entity.bankName,
                        branch=entity.branch,
                        accountType=entity.accountType
                    )
                }
            }.collect{emit(it)}
        }
    }

    override fun getCustomerDetails(id:Int):Flow<Customer?> {
        return flow {
            syncIfEmpty()
            val entity=customerDao.getCustomerById(id)
            if(entity!=null) {
                emit(Customer(
                    id=entity.id,
                    name=entity.name,
                    avatar=entity.avatar,
                    maskedAccount=entity.maskedAccount,
                    balance=entity.balance,
                    currency=entity.currency,
                    kycStatus=entity.kycStatus,
                    selfiePath=entity.selfiePath,
                    dob=entity.dob,
                    nationality=entity.nationality,
                    address=entity.address,
                    contact=entity.contact,
                    ifsc=entity.ifsc,
                    bankName=entity.bankName,
                    branch=entity.branch,
                    accountType=entity.accountType
                ))
            } else {
                emit(null)
            }
        }
    }

    override suspend fun getBankDetails(ifsc:String):BankDetails? {
        val cached=ifscDao.getByIfsc(ifsc)
        val cacheDuration=24*60*60*1000
        if(cached!=null&&System.currentTimeMillis()-cached.cachedAt<cacheDuration) {
            return BankDetails(cached.bank,cached.branch,cached.city,cached.state,cached.micr)
        }
        return try {
            val response=razorpayIfscApi.getBranchDetails(ifsc)

            val newCache=IfscCacheEntity(
                ifsc=ifsc,
                bank=response.BANK,
                branch=response.BRANCH,
                city=response.CITY,
                state=response.STATE,
                micr=response.MICR
            )

            ifscDao.insert(newCache)
            BankDetails(response.BANK,response.BRANCH,response.CITY,response.STATE,response.MICR)

        }catch(e:Exception) {
            e.printStackTrace()
            cached?.let{BankDetails(it.bank,it.branch,it.city,it.state,it.micr)}
        }
    }

    override suspend fun saveSelfie(id:Int,selfiePath:String) {
        kycDataStore.saveKyc(id,selfiePath)
        customerDao.markVerified(id,selfiePath,System.currentTimeMillis())
    }

    override fun isDarkTheme():Flow<Boolean> {
        return kycDataStore.isDarkTheme()
    }

    override suspend fun saveTheme(isDark:Boolean) {
        kycDataStore.saveTheme(isDark)
    }

}
