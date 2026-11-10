package com.wout.schedule.service

import com.wout.infra.openweather.client.OpenWeatherClient
import com.wout.infra.openweather.dto.request.AirPollutionApiRequest
import com.wout.infra.openweather.dto.request.WeatherApiRequest
import com.wout.infra.openweather.dto.response.AirComponents
import com.wout.infra.openweather.dto.response.AirPollutionData
import com.wout.infra.openweather.dto.response.AirPollutionResponse
import com.wout.infra.openweather.dto.response.AirQualityMain
import com.wout.infra.openweather.dto.response.Clouds
import com.wout.infra.openweather.dto.response.Coord
import com.wout.infra.openweather.dto.response.Main
import com.wout.infra.openweather.dto.response.OpenWeatherResponse
import com.wout.infra.openweather.dto.response.Sys
import com.wout.infra.openweather.dto.response.UVIndexResponse
import com.wout.infra.openweather.dto.response.Weather
import com.wout.infra.openweather.dto.response.Wind
import com.wout.schedule.mapper.WeatherApiMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OpenWeatherApiServiceTest {

    private val seoul = Coord(lon = 126.978, lat = 37.5665)

    private val weatherResponse = OpenWeatherResponse(
        coord = seoul,
        weather = listOf(Weather(id = 800, main = "Clear", description = "맑음", icon = "01d")),
        base = "stations",
        main = Main(temp = 23.5, feelsLike = 24.2, tempMin = 21.8, tempMax = 25.1, pressure = 1013, humidity = 65),
        wind = Wind(speed = 2.5),
        clouds = Clouds(all = 25),
        dt = 1716649200L,
        sys = Sys(country = "KR", sunrise = 1716629722L, sunset = 1716681483L),
        timezone = 32400,
        id = 1835848L,
        name = "Seoul",
        cod = 200
    )

    private val airResponse = AirPollutionResponse(
        coord = seoul,
        list = listOf(
            AirPollutionData(
                main = AirQualityMain(aqi = 2),
                components = AirComponents(
                    co = 320.5, no = 2.1, no2 = 15.3, o3 = 120.4,
                    so2 = 5.2, pm25 = 15.2, pm10 = 38.6, nh3 = 3.7
                ),
                dt = 1716649200L
            )
        )
    )

    private fun serviceWithUv(uv: () -> UVIndexResponse): OpenWeatherApiService {
        val client = object : OpenWeatherClient {
            override fun getCurrentWeather(request: WeatherApiRequest) = weatherResponse
            override fun getAirPollution(request: AirPollutionApiRequest) = airResponse
            override fun getUVIndex(request: WeatherApiRequest) = uv()
        }
        return OpenWeatherApiService(client, WeatherApiMapper(), "test-key")
    }

    @Test
    fun `UV 조회가 실패해도 도시의 날씨 데이터는 uvIndex 없이 반환된다`() {
        val service = serviceWithUv { throw RuntimeException("404 Not Found") }

        val data = service.fetchWeatherData(seoul.lat, seoul.lon, "서울")

        assertNull(data.uvIndex)
        assertEquals(23.5, data.temperature)
    }

    @Test
    fun `UV 조회가 성공하면 uvIndex에 응답 값이 담긴다`() {
        val service = serviceWithUv {
            UVIndexResponse(lat = seoul.lat, lon = seoul.lon, dateIso = "2025-05-25T12:00:00Z", date = 1716649200L, value = 7.5)
        }

        val data = service.fetchWeatherData(seoul.lat, seoul.lon, "서울")

        assertEquals(7.5, data.uvIndex)
    }
}
