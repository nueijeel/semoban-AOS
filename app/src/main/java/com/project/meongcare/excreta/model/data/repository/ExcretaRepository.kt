package com.project.meongcare.excreta.model.data.repository

import com.project.meongcare.excreta.model.entities.ExcretaDetailGetResponse
import com.project.meongcare.excreta.model.entities.ExcretaRecordGetRequest
import com.project.meongcare.excreta.model.entities.ExcretaRecordGetResponse
import com.project.meongcare.excreta.model.entities.ExcretaUploadRequest

interface ExcretaRepository {
    suspend fun postExcreta(excretaUploadRequest: ExcretaUploadRequest): Int?

    suspend fun getExcretaRecord(excretaRecordGetRequest: ExcretaRecordGetRequest): ExcretaRecordGetResponse?

    suspend fun getExcretaDetail(excretaId: Long): ExcretaDetailGetResponse?

    suspend fun deleteExcreta(excretaIds: IntArray): Int?

    suspend fun patchExcreta(excretaUploadRequest: ExcretaUploadRequest): Int?
}