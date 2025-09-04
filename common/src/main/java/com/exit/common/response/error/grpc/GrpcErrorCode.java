package com.exit.common.response.error.grpc;

import io.grpc.Status;

public interface GrpcErrorCode {
    Status.Code getGrpcStatusCode();
    String getDevelopCode();
}
