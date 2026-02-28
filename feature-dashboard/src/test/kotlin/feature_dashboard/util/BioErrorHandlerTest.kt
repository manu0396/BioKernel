// feature-dashboard/src/test/kotlin/feature_dashboard/util/BioErrorHandlerTest.kt
package feature_dashboard.util

import com.neogenesis.components.util.UiText
import com.neogenesis.domain.model.BioKernelException
import com.neogenesis.feature_dashboard.R
import org.junit.Assert.assertEquals
import org.junit.Test

class BioErrorHandlerTest {

    private val errorHandler = BioErrorHandler()

    @Test
    fun `map UnauthorizedException should return session expired resource`() {
        val exception = BioKernelException.UnauthorizedException()
        val result = errorHandler.map(exception)

        assert(result is UiText.StringResource)
        assertEquals(R.string.error_unauthorized, (result as UiText.StringResource).resId)
    }

    @Test
    fun `map ServerException should pass correct status code to resource`() {
        val code = 503
        val exception = BioKernelException.ServerException(code, "Service Unavailable")
        val result = errorHandler.map(exception)

        assert(result is UiText.StringResource)
        val resource = result as UiText.StringResource
        assertEquals(R.string.error_server_generic, resource.resId)
        assertEquals(code, resource.args[0])
    }
}