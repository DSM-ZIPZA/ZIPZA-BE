package com.example.zipzabe.global.feign.client

import com.example.zipzabe.global.feign.config.ApickFeignConfig
import feign.Response
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(
    name = "apick",
    url = "\${apick.url}",
    configuration = [ApickFeignConfig::class]
)
interface ApickClient {

    @PostMapping(
        value = ["/rest/building_register"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun getBuildingRegister(
        @RequestHeader("CL_AUTH_KEY") authKey: String,
        @RequestBody body: MultiValueMap<String, Any>
    ): Response
}
