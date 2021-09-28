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

package me.hufman.androidautoidrive.evplanning.iternio

import com.truchsess.evrouting.iternio.dto.VehicleLibraryResult
import io.sentry.Sentry
import me.hufman.androidautoidrive.carapp.L
import me.hufman.androidautoidrive.evplanning.iternio.api.PlanningAPI
import me.hufman.androidautoidrive.evplanning.iternio.dto.ChargersResult
import me.hufman.androidautoidrive.evplanning.iternio.dto.NetworksResult
import me.hufman.androidautoidrive.evplanning.iternio.dto.OutletsResult
import me.hufman.androidautoidrive.evplanning.iternio.dto.PlanRequest
import me.hufman.androidautoidrive.evplanning.iternio.entity.PlanResult
import me.hufman.androidautoidrive.evplanning.iternio.entity.toPlanResult
import me.hufman.androidautoidrive.evplanning.iternio.entity.toResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class GetChargerArgs(
        val lat: Double? = null,
        val lon: Double? = null,
        val radius: Double? = null,
        val types: List<String>? = null,
        val limit: Int? = null,
        val allowedDbs: List<String>? = null,
        val ids: List<Long>? = null,
        val getAmenities: Boolean = false,
        val amenityMaxDist: Double? = null,
        val amenityCateories: List<String>? = null,
        val amenityFoodTypes: List<String>? = null,
)

interface Planning {

    fun getVehicleLibrary(s: (VehicleLibraryResult) -> Unit, e: (String) -> Unit)

    fun getChargers(args: GetChargerArgs, s: (ChargersResult) -> Unit, e: (String) -> Unit)

    fun getOutletTypes(s: (OutletsResult) -> Unit, e: (String) -> Unit)

    fun getNetworks(s: (NetworksResult) -> Unit, e: (String) -> Unit)

    fun plan(planRequest: PlanRequest, s: (PlanResult) -> Unit, e: (String) -> Unit)

    fun planLight(planRequest: PlanRequest, s: (PlanResult) -> Unit, e: (String) -> Unit)
}

class PlanningImpl(baseUrl: String, private val authorization: String) : Planning {

    private val planningApi: PlanningAPI = PlanningAPI.create(baseUrl)

    private fun <R> apiCall(apiFun: () -> Call<R>, onSuccess: (R) -> Unit, onError: (String) -> Unit) {
        try {
            apiFun().enqueue(object : Callback<R> {
                override fun onResponse(call: Call<R>, response: Response<R>) {
                    val body: R? = response.body()
                    if (response.isSuccessful && body != null) {
                        onSuccess(body)
                    } else {
                        response.errorBody()?.string()?.let {
                            if (it.isNotEmpty()) {
                                onError(it)
                                return
                            }
                        }
                        response.message().let {
                            if (it.isNotEmpty()) {
                                onError(it)
                                return
                            }
                        }
                        onError(L.EVPLANNING_ERROR)
                    }
                }

                override fun onFailure(call: Call<R>, t: Throwable) {
                    t.localizedMessage?.let {
                        if (it.isNotEmpty()) {
                            onError(it)
                            return
                        }
                    }
                    onError(L.EVPLANNING_ERROR)
                }
            })
        } catch (t: Throwable) {
            Sentry.capture(t)
        }
    }

    override fun getVehicleLibrary(s: (VehicleLibraryResult) -> Unit, e: (String) -> Unit) = apiCall({ planningApi.getVehicleLibrary(authorization) }, s, e)

    override fun getChargers(args: GetChargerArgs, s: (ChargersResult) -> Unit, e: (String) -> Unit) = apiCall(
            { planningApi.getChargers(
                    args.lat,
                    args.lon,
                    args.radius,
                    args.types?.joinToString(","),
                    args.limit,
                    args.allowedDbs?.joinToString(","),
                    args.ids?.map { it.toString() }?.joinToString(","),
                    args.getAmenities,
                    args.amenityMaxDist?.toString(),
                    args.amenityCateories?.joinToString(","),
                    args.amenityFoodTypes?.joinToString(","),
                    authorization
            ) }, s, e)

    override fun getOutletTypes(s: (OutletsResult) -> Unit, e: (String) -> Unit) = apiCall({ planningApi.getOutletTypes(authorization) }, s, e)

    override fun getNetworks(s: (NetworksResult) -> Unit, e: (String) -> Unit) = apiCall({ planningApi.getNetworks(authorization) }, s, e)

    override fun plan(planRequest: PlanRequest, s: (PlanResult) -> Unit, e: (String) -> Unit) = apiCall({ planningApi.plan(planRequest, authorization) }, {
        s(toResult(it))
    }, e)

    override fun planLight(planRequest: PlanRequest, s: (PlanResult) -> Unit, e: (String) -> Unit) = apiCall({ planningApi.planLight(planRequest, authorization) }, {
        s(toPlanResult(it))
    }, e)
}