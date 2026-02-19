package com.neogenesis.domain.model

sealed class NetworkResult<out T> {
    data object Loading : NetworkResult<Nothing>()
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: BioKernelException) : NetworkResult<Nothing>()
}