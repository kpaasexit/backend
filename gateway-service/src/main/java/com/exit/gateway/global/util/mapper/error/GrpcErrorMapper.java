package com.exit.gateway.global.util.mapper.error;

import com.exit.common.response.error.rest.ErrorCode;

public interface GrpcErrorMapper {

    ErrorCode mapToErrorCode(String developCode, String grpcStatusCode);

    ErrorCode getDefaultErrorCode();

}
