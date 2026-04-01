package com.example.zipzabe.global.error

import com.example.zipzabe.global.error.exception.ErrorCode
import com.example.zipzabe.global.error.exception.ErrorResponse
import com.example.zipzabe.global.error.exception.ZipzaException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

class GlobalExceptionFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: ZipzaException) {
            writeErrorResponse(response, e.errorCode)
        } catch (e: Exception) {
            writeErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR)
        }
    }

    private fun writeErrorResponse(response: HttpServletResponse, errorCode: ErrorCode) {
        val errorResponse = ErrorResponse(errorCode.httpStatus, errorCode.message)
        response.status = errorCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
