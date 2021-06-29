/**********************************************************************************************
Copyright (C) 2021 Norbert Truchsess norbert.truchsess@t-online.de

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **********************************************************************************************/

package me.hufman.androidautoidrive.evplanning.iternio.api

import com.truchsess.evrouting.iternio.dto.*
import me.hufman.androidautoidrive.evplanning.iternio.dto.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface PlanningAPI {

    // API Gateway
    // val API_SERVER: String = "https://api.iternio.com"

    // Accept: application/json, text/plain, */*
    // Accept-Encoding: gzip, deflate, br
    // Accept-Language: de,en-US;q=0.7,en;q=0.3
    // Authorization: APIKEY f4128c06-5e39-4852-95f9-3286712a9f3a
    // Cache-Control: no-cache
    // Connection: keep-alive
    // Content-Length: 1067
    // Content-Type: application/json;charset=utf-8
    // DNT:	1
    // Host: api.iternio.com
    // Origin:	https://abetterrouteplanner.com
    // Pragma:	no-cache
    // Referer:	https://abetterrouteplanner.com/?plan_uuid=e2c2af36-e814-4717-8d00-35fe86699bc5
    // TE: Trailers
    // User-Agent:	Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:86.0) Gecko/20100101 Firefox/86.0

    // Authentication is done vida the URL query parameter "api_key=xxxx" or
    // the HTTP header "Authorization" with the value "APIKEY xxxxx".
    // You can obtain API keys and more information by contacting us at contact@iternio.com

    @GET("1/get_vehicle_library")
    fun getVehicleLibrary(// @Field("usable_battery") boolean usableBattery,
            @Header("Authorization") authorization: String): Call<VehicleLibraryResult>

    @GET("1/get_chargers")
    fun getChargers(@Query("lat") lat: Double?,                       // either lat,lon,radius,types,limit or ids, not both
                    @Query("lon") lon: Double?,
                    @Query("radius") radius: Double?,                 // radius in meters
                    @Query("types") types: String?,                   // ccs,type2 - comma-separated list of outlet-types
                    @Query("limit") limit: Int?,                      // maximum number of results
                    @Query("allowed_dbs") allowedDbs: String?,        // optional, unconfirmed though...
                    @Query("ids") ids: String?,                       // comma-separated list of charger-ids
                    @Query("get_amenities") get_amenities: Boolean = false,
                    @Query("amenity_maxdist") amenity_maxdist: String?,
                    @Query("amenity_categories") amenity_categories: String?,
                    @Query("amenity_foodtypes") amenity_foodtypes: String?,
                    @Header("Authorization") authorization: String): Call<ChargersResult>

    @GET("1/get_outlet_types")
    fun getOutletTypes(@Header("Authorization") authorization: String): Call<OutletsResult>

    @GET("1/get_networks")
    fun getNetworks(@Header("Authorization") authorization: String): Call<NetworksResult>

    @POST("1/plan")
    fun plan(@Body planRequest: PlanRequest,
             @Header("Authorization") authorization: String): Call<PlanResult>

    @POST("1/plan_light")
    fun planLight(@Body planRequest: PlanRequest,
                  @Header("Authorization") authorization: String): Call<PlanResult>

    companion object {
        fun create(baseUrl: String): PlanningAPI {

            val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()
            val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

            return retrofit.create(PlanningAPI::class.java)
        }
    }
}
