package `in`.mato.signzy.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.mato.signzy.data.remote.DummyJsonApi
import `in`.mato.signzy.data.remote.RazorpayIfscApi
import `in`.mato.signzy.data.repository.CustomerRepositoryImpl
import `in`.mato.signzy.domain.repository.CustomerRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient():OkHttpClient {
        val logging=HttpLoggingInterceptor().apply{
            level=HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }


    @Provides
    @Singleton
    fun provideDummyJsonApi(okHttpClient:OkHttpClient):DummyJsonApi {
        return Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DummyJsonApi::class.java)

    }

    @Provides
    @Singleton
    fun provideRazorpayIfscApi(okHttpClient:OkHttpClient):RazorpayIfscApi {
        return Retrofit.Builder()
            .baseUrl("https://ifsc.razorpay.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RazorpayIfscApi::class.java)

    }

    @Provides
    @Singleton
    fun provideCustomerRepository(
        customerDao:`in`.mato.signzy.data.local.CustomerDao,
        ifscDao:`in`.mato.signzy.data.local.IfscDao,
        dummyJsonApi:DummyJsonApi,
        razorpayIfscApi:RazorpayIfscApi,
        kycDataStore:`in`.mato.signzy.data.local.KycDataStore
    ):CustomerRepository {
        return CustomerRepositoryImpl(customerDao,ifscDao,dummyJsonApi,razorpayIfscApi,kycDataStore)
    }

}
