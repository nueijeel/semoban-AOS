package com.project.meongcare.info.model.data.remote

import com.project.meongcare.home.model.entities.GetDogListResponse
import com.project.meongcare.home.model.entities.GetUserProfileResponse
import com.project.meongcare.info.model.entities.GetDogInfoResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApi {
    @GET("/member/profile")
    suspend fun getUserProfile(
        @Header("AccessToken") accessToken: String,
    ): Response<GetUserProfileResponse>

    @GET("/dog")
    suspend fun getDogList(
        @Header("AccessToken") accessToken: String,
    ): Response<GetDogListResponse>

    @GET("/dog/{dogId}")
    suspend fun getDogInfo(
        @Path("dogId") dogId: Long,
        @Header("AccessToken") accessToken: String,
    ): Response<GetDogInfoResponse>

    @DELETE("/dog/{dogId}")
    suspend fun deleteDog(
        @Path("dogId") dogId: Long,
        @Header("AccessToken") accessToken: String,
    ): Response<Int>

    @Multipart
    @PUT("/dog/{dogId}")
    suspend fun putDogInfo(
        @Path("dogId") dogId: Long,
        @Header("AccessToken") accessToken: String,
        @Part file: MultipartBody.Part,
        @Part("dto") dto: RequestBody,
    ): Response<Int>

    @DELETE("/auth/logout")
    suspend fun logoutUser(
        @Header("RefreshToken") refreshToken: String,
    ): Response<Int>

    @DELETE("/member")
    suspend fun deleteUser(
        @Header("AccessToken") accessToken: String,
    ): Response<Int>

    @PATCH("/member/alarm")
    suspend fun patchPushAgreement(
        @Query("pushAgreement") pushAgreement: Boolean,
        @Header("AccessToken") accessToken: String,
    ): Response<Int>

    @Multipart
    @PATCH("member/profile")
    suspend fun patchProfileImage(
        @Header("AccessToken") accessToken: String,
        @Part file: MultipartBody.Part,
    ): Response<Int>
}