package mihir.storage_demo

import mihir.storage_demo.Config.UPLOAD_GIF
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface ApiService {

    @Multipart
    @POST(UPLOAD_GIF)
    fun uploadGif(@Part file: MultipartBody.Part): Call<ResponseBody>


    @Multipart
    @Headers("apiKey:MrE+Qh3ul+1433YYth51QlJ3Ja35Ayhn4dPOrIuBMOM=")
    @POST("api/wpsisapi/v1/profilePicUpload")
    fun getUpdateProfileImage(
        @Part bodyImage: MultipartBody.Part?,
        @Query("deviceId") deviceId: String?,
        @Query("deviceType") deviceType: String?
    ): Call<ResponseBody>


    @GET
    fun downloadFileWithDynamicUrlSync(@Url fileUrl: String): Call<ResponseBody>
}