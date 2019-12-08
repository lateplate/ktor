package com.dankim

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GoogleServices {
  @GET("gmail/v1/users/{userName}/threads")
  fun getGmailThreads(@Path("userName") userName: String): Call<GmailThreads>

  @GET("tasks/v1/users/@me/lists")
  fun getTaskLists(): Call<GoogleTaskLists>

  @GET("tasks/v1/lists/{listId}/tasks")
  fun getTasks(@Path("listId") listId: String): Call<GoogleTasks>

  @POST("tasks/v1/lists/{listId}/tasks")
  fun createTask(@Path("listId") listId: String, @Body task: GoogleTask): Call<ResponseBody>
}

fun getGoogleServicesInstance(): GoogleServices {
  val httpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor())
    .build()

  return Retrofit.Builder()
    .baseUrl("https://www.googleapis.com/")
    .addConverterFactory(MoshiConverterFactory.create())
    .client(httpClient)
    .build()
    .create()
}

inline fun <reified T> T.toJson(): String {
  val adapter = Moshi.Builder().build().adapter(T::class.java)
  return adapter.toJson(this)
}

class AuthInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val original = chain.request()

    val request = original.newBuilder()
      .header("Authorization", "Bearer $accessToken")
      .build()

    return chain.proceed(request)
  }
}