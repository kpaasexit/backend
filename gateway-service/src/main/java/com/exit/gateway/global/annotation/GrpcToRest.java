package com.exit.gateway.global.annotation;

import com.exit.gateway.global.util.mapper.error.DefaultGrpcErrorMapper;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,  ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcToRest {
    Class<? extends GrpcErrorMapper> mapper() default DefaultGrpcErrorMapper.class;
}
