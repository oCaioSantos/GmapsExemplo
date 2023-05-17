package com.example.gmaps.api

import com.example.gmaps.models.NearbySearchResponse
import com.example.gmaps.models.Place
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NearbySearchCallback(
    private val addMakers: (List<Place>) -> Unit,
    private val handleError: (String?) -> Unit
) : Callback<NearbySearchResponse> {
    override fun onResponse(
        call: Call<NearbySearchResponse>,
        response: Response<NearbySearchResponse>
    ) {
        if (response.isSuccessful) {
            response.body()?.results?.let {
                addMakers(it)
            }
        } else {
            handleError(response.body()?.error_message)
        }
    }

    override fun onFailure(call: Call<NearbySearchResponse>, t: Throwable) {
        handleError(t.message)
    }
}