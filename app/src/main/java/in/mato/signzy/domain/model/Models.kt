package `in`.mato.signzy.domain.model

data class Customer(
    val id:Int,
    val name:String,
    val avatar:String,
    val maskedAccount:String,
    val balance:Double,
    val currency:String,
    val kycStatus:String,
    val selfiePath:String?,
    val dob:String,
    val nationality:String,
    val address:String,
    val contact:String,
    val ifsc:String,
    val bankName:String?,
    val branch:String?,
    val accountType:String
)

data class BankDetails(
    val bank:String,
    val branch:String,
    val city:String,
    val state:String,
    val micr:String?
)
