package com.wout.infra.openweather.config

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * packageName    : com.wout.infra.openweather.config
 * fileName       : OpenWeatherClientConfig
 * author         : MinKyu Park
 * date           : 25. 5. 21.
 * description    : OpenWeather Feign 클라이언트 설정
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 21.        MinKyu Park       최초 생성
 */
@Configuration
class OpenWeatherClientConfig {

    @Bean
    fun errorDecoder(): ErrorDecoder = ErrorDecoder { methodKey, response ->
        val body = response.body()?.asReader(Charsets.UTF_8)?.use { it.readText() }?.trim() ?: ""
        ApiException(ErrorCode.WEATHER_API_ERROR, "OpenWeather ${response.status()} $methodKey: $body")
    }
}
