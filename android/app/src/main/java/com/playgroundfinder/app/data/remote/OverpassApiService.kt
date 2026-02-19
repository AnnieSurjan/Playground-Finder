package com.playgroundfinder.app.data.remote

import com.playgroundfinder.app.data.remote.dto.OverpassResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OverpassApiService {

    @FormUrlEncoded
    @POST("interpreter")
    suspend fun queryPlaygrounds(
        @Field("data") query: String
    ): OverpassResponse

    companion object {
        /**
         * Overpass QL lekérdezés: node és way elemek "leisure=playground" taggel,
         * adott sugáron belül (lat,lng körül).
         * Az "out center" biztosítja, hogy a way-ekhez is kapunk középpontot.
         */
        fun buildQuery(lat: Double, lng: Double, radiusMeters: Int): String =
            """
            [out:json][timeout:25];
            (
              node["leisure"="playground"](around:$radiusMeters,$lat,$lng);
              way["leisure"="playground"](around:$radiusMeters,$lat,$lng);
            );
            out center body;
            """.trimIndent()
    }
}
