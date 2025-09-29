package com.exit.notification.service;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class NotificationGrpcService {
    private final NotificationService notificationService;
}
