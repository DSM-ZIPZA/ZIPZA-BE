package com.example.zipzabe

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@EnableFeignClients
@SpringBootApplication
class ZipzaBeApplication

fun main(args: Array<String>) {
    runApplication<ZipzaBeApplication>(*args)
}
