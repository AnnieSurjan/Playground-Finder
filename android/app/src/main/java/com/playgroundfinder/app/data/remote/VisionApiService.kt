package com.playgroundfinder.app.data.remote

import com.playgroundfinder.app.data.remote.dto.VisionAnnotateRequest
import com.playgroundfinder.app.data.remote.dto.VisionAnnotateResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface VisionApiService {

    /**
     * Képek annotálása a Cloud Vision API-val.
     * Endpoint: https://vision.googleapis.com/v1/images:annotate
     *
     * @param apiKey  Google Cloud API kulcs (Maps API kulccsal azonos, ha a Vision API engedélyezve van)
     * @param request  A kérés: képlista + kért funkciók (LABEL_DETECTION, OBJECT_LOCALIZATION)
     */
    @POST("v1/images:annotate")
    suspend fun annotateImages(
        @Query("key") apiKey: String,
        @Body request: VisionAnnotateRequest
    ): VisionAnnotateResponse
}
