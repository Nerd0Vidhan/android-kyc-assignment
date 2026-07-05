package `in`.mato.signzy.ui.digitalBank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mato.signzy.domain.model.BankDetails
import `in`.mato.signzy.domain.model.Customer
import `in`.mato.signzy.domain.repository.CustomerRepository
import `in`.mato.signzy.ui.mlKit.DownloadState
import `in`.mato.signzy.ui.mlKit.MlKitDownloader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DigitalBankViewModel @Inject constructor(
    private val repository:CustomerRepository,
    private val downloader:MlKitDownloader):ViewModel()
{

    private val _viewState=MutableStateFlow(DigitalBankViewState())
    val viewState:StateFlow<DigitalBankViewState> =_viewState
    private val queryFlow=MutableStateFlow("")
    private val accountTypeFlow=MutableStateFlow("All")
    private val tabFlow=MutableStateFlow("PENDING")
    @OptIn(FlowPreview::class)
    val customersPagingFlow:Flow<PagingData<Customer>> =combine(
        queryFlow.debounce(400.milliseconds),
        accountTypeFlow,
        tabFlow
    ){q,t,tab->Triple(q,t,tab)}.flatMapLatest{filters->
        repository.getCustomerList(filters.first,filters.second,filters.third)
    }.cachedIn(viewModelScope)


    init {
        viewModelScope.launch {
            downloader.downloadState.collect{ds->
                _viewState.value=_viewState.value.copy(modelDownloadState=ds)
            }
        }
        viewModelScope.launch {
            repository.isDarkTheme().collect{isDark->
                _viewState.value=_viewState.value.copy(isDarkTheme=isDark)
            }
        }
        checkMlKitAvailability()
    }

    fun handleIntent(intent:DigitalBankIntent) {
        when(intent) {
            is DigitalBankIntent.Search-> {
                queryFlow.value=intent.query
                _viewState.value=_viewState.value.copy(query=intent.query)
            }
            is DigitalBankIntent.ChangeTab-> {
                tabFlow.value=intent.tab
                _viewState.value=_viewState.value.copy(selectedTab=intent.tab)
            }
            is DigitalBankIntent.SelectAccountType-> {
                accountTypeFlow.value=intent.type
                _viewState.value=_viewState.value.copy(selectedAccountType=intent.type)
            }
            is DigitalBankIntent.SelectCustomer-> loadCustomerDetails(intent.customerId)

            is DigitalBankIntent.CloseDetails-> {
                _viewState.value=_viewState.value.copy(selectedCustomer=null,bankDetails=null)
            }
            is DigitalBankIntent.ResolveIfsc->
                resolveIfscCode(intent.ifsc)

            is DigitalBankIntent.CompleteKyc->
                completeKycStatus(intent.customerId,intent.selfiePath)
            is DigitalBankIntent.ToggleTheme-> {
                viewModelScope.launch {
                    repository.saveTheme(intent.isDark)
                }
            }
            is DigitalBankIntent.RefreshCustomers-> {
                refreshCustomers()
            }
            is DigitalBankIntent.TriggerModelDownload-> {
                downloader.downloadModel(intent.onSuccess)
            }
        }
    }
    private fun loadCustomerDetails(id:Int) {
        viewModelScope.launch {
            _viewState.value=_viewState.value.copy(isLoadingDetails=true)
            repository.getCustomerDetails(id).collect{cust->
                if(cust!=null) {
                    _viewState.value=_viewState.value.copy(
                        selectedCustomer=cust,
                        isLoadingDetails=false
                    )
                    resolveIfscCode(cust.ifsc)
                } else {
                    _viewState.value=_viewState.value.copy(
                        isLoadingDetails=false,
                        errorMessage="Customer not found"
                    )
                }
            }
        }
    }

    private fun resolveIfscCode(ifsc:String) {
        viewModelScope.launch {
            val details=repository.getBankDetails(ifsc)
            _viewState.value=_viewState.value.copy(bankDetails=details)
        }
    }

    private fun completeKycStatus(id:Int,selfiePath:String) {
        viewModelScope.launch {
            repository.saveSelfie(id,selfiePath)
            repository.getCustomerDetails(id).collect{cust->
                if(cust!=null) {
                    _viewState.value=_viewState.value.copy(
                        selectedCustomer=cust
                    )
                }
            }
        }
    }

    private fun refreshCustomers() {
        viewModelScope.launch {
            _viewState.value=_viewState.value.copy(isRefreshing=true,errorMessage=null)
            try {
                repository.refreshCustomers()
            }catch(e:Exception) {
                _viewState.value=_viewState.value.copy(errorMessage=e.message)
            }finally {
                _viewState.value=_viewState.value.copy(isRefreshing=false)
            }
        }
    }

    private fun checkMlKitAvailability() {
        downloader.checkAvailability{isAvailable->
            if(isAvailable) {
                _viewState.value=_viewState.value.copy(modelDownloadState=DownloadState.Completed)
            }
        }
    }
}


data class DigitalBankViewState(
    val query:String="",
    val selectedTab:String="PENDING",
    val selectedAccountType:String="All",
    val selectedCustomer:Customer?=null,
    val isLoadingDetails:Boolean=false,
    val bankDetails:BankDetails?=null,
    val errorMessage:String?=null,
    val modelDownloadState:DownloadState=DownloadState.Idle,
    val isRefreshing:Boolean=false,
    val isDarkTheme:Boolean=false
)
