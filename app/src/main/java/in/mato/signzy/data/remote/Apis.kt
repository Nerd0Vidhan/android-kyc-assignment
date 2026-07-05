package `in`.mato.signzy.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class DummyJsonUsersResponse(
    val users:List<DummyUser>,
    val total:Int,
    val skip:Int,
    val limit:Int
)

data class DummyUser(
    val id:Int,
    val firstName:String,
    val lastName:String,
    val email:String,
    val phone:String,
    val birthDate:String,
    val gender:String,
    val image:String,
    val address:DummyAddress,
    val bank:DummyBank
)

data class DummyAddress(
    val address:String,
    val city:String,
    val state:String,
    val country:String
)

data class DummyBank(
    val cardExpire:String,
    val cardNumber:String,
    val cardType:String,
    val currency:String,
    val iban:String
)

data class RazorpayIfscResponse(
    val BANK:String,
    val BRANCH:String,
    val CITY:String,
    val STATE:String,
    val MICR:String?
)

interface DummyJsonApi {
    @GET("users")
    suspend fun getUsers(
        @Query("limit") limit:Int,
        @Query("skip") skip:Int
    ):DummyJsonUsersResponse
}

interface RazorpayIfscApi {
    @GET("{ifsc}")
    suspend fun getBranchDetails(
        @Path("ifsc") ifsc:String
    ):RazorpayIfscResponse
}
