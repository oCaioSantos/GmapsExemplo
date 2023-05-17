package com.example.gmaps.models

data class NearbySearchResponse(
    val results: List<Place>,
    val error_message: String,
    val status: String,
)

data class Place(
    val business_status: String,
    val geometry: PlaceGeometry,
    val icon: String,
    val icon_background_color: String,
    val icon_mask_base_uri: String,
    val name: String,
    val place_id: String,
    val rating: Double,
    val user_ratings_total: Int,
    val vicinity: String,
)

data class PlaceGeometry(
    val location: PlaceLocation
)

data class PlaceLocation(
    val lat: Double,
    val lng: Double
)
