package com.example.zipzabe.global.feign.config

import com.example.zipzabe.global.error.exception.ErrorCode
import com.example.zipzabe.global.error.exception.ZipzaException
import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.stereotype.Component

@Component
class FeignErrorDecoder : ErrorDecoder {

    override fun decode(methodKey: String, response: Response): Exception {
        return when (response.status()) {
            400 -> FeignClientException(ErrorCode.EXTERNAL_API_BAD_REQUEST)
            401 -> FeignClientException(ErrorCode.EXTERNAL_API_UNAUTHORIZED)
            404 -> FeignClientException(ErrorCode.EXTERNAL_API_NOT_FOUND)
            else -> FeignClientException(ErrorCode.EXTERNAL_API_ERROR)
        }
    }
}
class FeignClientException(errorCode: ErrorCode) : ZipzaException(errorCode)
