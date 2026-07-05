package `in`.mato.signzy.ui.digitalBank

sealed class DigitalBankIntent {

    data class Search(val query:String):DigitalBankIntent()
    data class ChangeTab(val tab:String):DigitalBankIntent()
    data class SelectAccountType(val type:String):DigitalBankIntent()
    data class SelectCustomer(val customerId:Int):DigitalBankIntent()
    object CloseDetails:DigitalBankIntent()
    data class ResolveIfsc(val ifsc:String):DigitalBankIntent()
    data class CompleteKyc(val customerId:Int,val selfiePath:String):DigitalBankIntent()
    data class TriggerModelDownload(val onSuccess:()->Unit):DigitalBankIntent()

    data class ToggleTheme(val isDark:Boolean):DigitalBankIntent()
    object RefreshCustomers:DigitalBankIntent()
}