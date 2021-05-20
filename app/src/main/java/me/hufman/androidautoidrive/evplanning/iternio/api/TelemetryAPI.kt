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

package me.hufman.androidautoidrive.evplanning.iternio.api;

import me.hufman.androidautoidrive.evplanning.iternio.dto.CarModelsListResult
import me.hufman.androidautoidrive.evplanning.iternio.dto.NextChargeResult
import me.hufman.androidautoidrive.evplanning.iternio.dto.PlanResult
import me.hufman.androidautoidrive.evplanning.iternio.dto.TelemetryRequest
import me.hufman.androidautoidrive.evplanning.iternio.dto.TelemetryResult

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

const val API_SERVER = "https://api.iternio.com"

interface TelemetryAPI {

    // API Gateway
    // public static String API_SERVER = "https://api.iternio.com";

    // Authentication is done vida the URL query parameter "api_key=xxxx" or
    // the HTTP header "Authorization" with the value "APIKEY xxxxx".
    // You can obtain API keys and more information by contacting us at contact@iternio.com

    @POST("1/tlm/send")
    fun send(@Field("token") token: String,
             @Body parameters: TelemetryRequest,
             @Header("Authorization") authorization: String): Call<TelemetryResult>

    @GET("1/tlm/get_carmodels_list")
    fun getCarModelsList(@Header("Authorization") authorization: String): Call<CarModelsListResult>

    @GET("1/tlm/get_next_charge")
    fun getNextCharge(@Field("token") token: String,
                      @Header("Authorization") authorization: String): Call<NextChargeResult>

    @GET("1/tlm/set_next_charge")
    fun setNextCharge(@Field("token") token: String,
                      @Field("next_charge_to_perc") nextChargeToPerc: Double,
                      @Header("Authorization") authorization: String): Call<Void>

    @GET("1/tlm/get_latest_plan")
    fun getLatestPlan(@Field("token") token: String,
                      @Header("Authorization") authorization: String): Call<PlanResult>
}
