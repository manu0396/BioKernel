package feature_dashboard.util

import com.neogenesis.components.util.UiText
import com.neogenesis.domain.model.BioKernelException
import com.neogenesis.feature_dashboard.R

class BioErrorHandler {
    fun map(exception: Throwable?): UiText {
        return when (exception) {
            is BioKernelException.UnauthorizedException -> {
                UiText.StringResource(R.string.error_unauthorized)
            }
            is BioKernelException.NoNetworkException -> {
                UiText.StringResource(R.string.error_no_network)
            }
            is BioKernelException.ServerException -> {
                UiText.StringResource(R.string.error_server_generic, exception.code, exception.serverMessage)
            }
            else -> {
                UiText.StringResource(R.string.error_unknown, exception?.message ?: "Desconocido")
            }
        }
    }
}