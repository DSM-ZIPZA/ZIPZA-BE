package com.example.zipzabe.domain.user.controller

import com.example.zipzabe.domain.user.dto.UserInfoResponse
import com.example.zipzabe.domain.user.service.QueryUserInfoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val queryUserInfoService: QueryUserInfoService
) {

    @GetMapping("/me")
    fun getUserInfo(): UserInfoResponse {
        return queryUserInfoService.execute()
    }
}