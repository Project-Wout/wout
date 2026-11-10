package com.wout.infra.openweather.config

import feign.Request
import feign.Response
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenWeatherErrorDecoderTest {

    private val methodKey = "OpenWeatherClient#getCurrentWeather(WeatherApiRequest)"

    private val request = Request.create(
        Request.HttpMethod.GET,
        "https://api.openweathermap.org/data/2.5/weather?appid=SECRET-KEY&lat=37.5&lon=127.0",
        emptyMap(),
        null,
        Charsets.UTF_8,
        null
    )

    private val decoder = OpenWeatherClientConfig().errorDecoder()

    @Test
    fun `오류 메시지에 상태 코드와 응답 본문은 담기고 API 키는 담기지 않는다`() {
        val response = Response.builder()
            .status(401)
            .reason("Unauthorized")
            .request(request)
            .body("""{"cod":401, "message": "Invalid API key."}""", Charsets.UTF_8)
            .build()

        val message = decoder.decode(methodKey, response).message!!

        assertFalse(message.contains("SECRET-KEY"), message)
        assertTrue(message.contains("401"), message)
        assertTrue(message.contains("Invalid API key"), message)
    }

    @Test
    fun `응답 본문이 없어도 오류 메시지에 요청 URL은 담기지 않는다`() {
        val response = Response.builder()
            .status(500)
            .reason("Internal Server Error")
            .request(request)
            .build()

        val message = decoder.decode(methodKey, response).message!!

        assertFalse(message.contains(request.url()), message)
    }
}
