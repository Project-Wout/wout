package com.wout.common.exception

import com.wout.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException

/**
 * packageName    : com.wout.common.exception
 * fileName       : GlobalExceptionHandler
 * author         : MinKyu Park
 * date           : 25. 5. 21.
 * description    : 전역 예외 처리 핸들러
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 21.        MinKyu Park       최초 생성
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * API 예외 처리
     */
    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException): ResponseEntity<ApiResponse<Nothing>> {
        log.error("API 예외 발생: {}", e.message, e)

        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode.code, e.message ?: e.errorCode.message))
    }

    /**
     * 유효성 검사 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    fun handleValidationException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("유효성 검사 예외 발생: {}", e.message, e)

        val errorCode = ErrorCode.INVALID_INPUT_VALUE
        val errorMessage = when (e) {
            is MethodArgumentNotValidException -> {
                e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            }
            is BindException -> {
                e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            }
            else -> errorCode.message
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorCode.code, errorMessage))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ApiResponse<Nothing>> {
        log.error("핸들러 메서드 유효성 검사 예외 발생: {}", e.message, e)

        val errorCode = ErrorCode.INVALID_INPUT_VALUE

        // Spring 6.x의 ParameterValidationResult에서 에러 메시지 추출
        val errorMessage = e.allValidationResults
            .flatMap { result ->
                result.resolvableErrors.map { error ->
                    val parameterName = result.methodParameter.parameterName ?: "unknown"
                    "$parameterName: ${error.defaultMessage}"
                }
            }
            .joinToString(", ")
            .ifEmpty { errorCode.message }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorCode.code, errorMessage))
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("예외 발생: {}", e.message, e)

        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(errorCode.code, errorCode.message))
    }
}