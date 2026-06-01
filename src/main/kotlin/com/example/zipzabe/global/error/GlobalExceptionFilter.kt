package com.example.zipzabe.global.error

import com.example.zipzabe.global.error.exception.ErrorCode
import com.example.zipzabe.global.error.exception.ErrorResponse
import com.example.zipzabe.global.error.exception.ZipzaException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

class GlobalExceptionFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(GlobalExceptionFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: ZipzaException) {
            log.warn(
                "Filtered ZipzaException. method={} uri={} query={} status={} errorCode={} remoteAddr={} userAgent={}",
                request.method,
                request.requestURI,
                request.queryString,
                e.errorCode.httpStatus.value(),
                e.errorCode.name,
                request.remoteAddr,
                request.getHeader("User-Agent"),
                e,
            )
            writeErrorResponse(response, e.errorCode)
        } catch (e: Exception) {
            log.error(
                "Filtered unhandled exception. method={} uri={} query={} status={} remoteAddr={} userAgent={}",
                request.method,
                request.requestURI,
                request.queryString,
                ErrorCode.INTERNAL_SERVER_ERROR.httpStatus.value(),
                request.remoteAddr,
                request.getHeader("User-Agent"),
                e,
            )
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
