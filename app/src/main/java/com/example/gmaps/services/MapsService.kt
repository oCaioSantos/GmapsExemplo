package com.example.gmaps.services

import com.example.gmaps.models.NearbySearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MapsService {

    @GET("maps/api/place/nearbysearch/json")
    fun findNearbyPlaces(
        @Query("location", encoded = false) location: String,
        @Query("radius", encoded = false) radius: Int,
        @Query("keyword", encoded = false) keyword: String,
        @Query("key", encoded = false) key: String,
    ): Call<NearbySearchResponse>

}