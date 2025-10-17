package com.exit.gateway.global.aop;

import com.exit.common.exception.grpc.GrpcExceptionResponseBody;
import com.exit.common.exception.rest.RestApiException;
import com.exit.common.response.error.rest.ErrorCode;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GrpcToRestExceptionAspect {
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    @Around("@annotation(com.exit.gateway.global.annotation.GrpcToRest) || " +
            "@within(com.exit.gateway.global.annotation.GrpcToRest)")
    public Object handleGrpcException(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();

        } catch (StatusRuntimeException e) {
            throw convertToRestApiException(joinPoint, e);
        }
    }

    private RestApiException convertToRestApiException(
            ProceedingJoinPoint joinPoint,
            StatusRuntimeException e
    ) {
        try {
            GrpcToRest annotation = getAnnotation(joinPoint);

            if (annotation == null) {
                log.warn("@GrpcToRest annotation not found, using default exception");
                return createDefaultException(e);
            }

            GrpcErrorMapper mapper = applicationContext.getBean(annotation.mapper());
            GrpcExceptionResponseBody grpcError = parseGrpcError(e);
            ErrorCode errorCode = mapper.mapToErrorCode(
                    grpcError.getDevelopCode(),
                    grpcError.getErrorCode()
            );

            log.warn("gRPC error converted to REST: {} -> {}",
                    grpcError.getDevelopCode(),
                    errorCode.getDevelopCode());

            return new RestApiException(errorCode, grpcError.getErrorMessage());

        } catch (Exception parseException) {
            log.error("Failed to parse gRPC error. Original error: {}", e.getMessage(), parseException);
            return createDefaultException(e);
        }
    }

    private GrpcToRest getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 메서드 레벨 어노테이션 확인
        GrpcToRest methodAnnotation = method.getAnnotation(GrpcToRest.class);
        if (methodAnnotation != null) {
            log.debug("Using method-level @GrpcToRest annotation for {}", method.getName());
            return methodAnnotation;
        }

        // 2. 클래스 레벨 어노테이션 확인
        Class<?> targetClass = joinPoint.getTarget().getClass();
        GrpcToRest classAnnotation = targetClass.getAnnotation(GrpcToRest.class);
        if (classAnnotation != null) {
            log.debug("Using class-level @GrpcToRest annotation for {}", targetClass.getSimpleName());
            return classAnnotation;
        }

        return null;
    }

    private GrpcExceptionResponseBody parseGrpcError(StatusRuntimeException e) {
        String description = e.getStatus().getDescription();

        if (description == null || description.isEmpty()) {
            log.warn("gRPC error description is empty, using status code: {}", e.getStatus().getCode());
            throw new IllegalArgumentException("gRPC error description is empty");
        }

        try {
            GrpcExceptionResponseBody body = objectMapper.readValue(description, GrpcExceptionResponseBody.class);
            log.debug("Parsed gRPC error: errorCode={}, developCode={}, message={}",
                    body.getErrorCode(), body.getDevelopCode(), body.getErrorMessage());
            return body;
        } catch (Exception ex) {
            log.error("Failed to parse gRPC error description as JSON: {}", description, ex);
            throw new IllegalArgumentException("Invalid gRPC error format", ex);
        }
    }

    private RestApiException createDefaultException(StatusRuntimeException e) {
        ErrorCode defaultError = new ErrorCode() {
            @Override
            public String getDevelopCode() {
                return "GRPC_ERR_999";
            }

            @Override
            public HttpStatus getHttpStatus() {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            @Override
            public String getErrorDescription() {
                return "gRPC 통신 중 알 수 없는 오류가 발생했습니다.";
            }
        };

        return new RestApiException(defaultError, e.getMessage());
    }
}