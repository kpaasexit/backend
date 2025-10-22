package com.exit.gateway.restdocs;

import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Multipart 요청의 form parameter를 문서화하는 커스텀 snippet
 */
public class MultipartFormParametersSnippet extends TemplatedSnippet {

    private final List<MultipartParameterDescriptor> descriptors;

    protected MultipartFormParametersSnippet(List<MultipartParameterDescriptor> descriptors) {
        super("multipart-form-parameters", null);
        this.descriptors = descriptors;
        for (MultipartParameterDescriptor descriptor : descriptors) {
            Assert.hasText(descriptor.getName(), "Parameter name must not be null or empty");
            Assert.notNull(descriptor.getDescription(), "Parameter description must not be null");
        }
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        List<Map<String, Object>> parameterList = new ArrayList<>();

        for (MultipartParameterDescriptor descriptor : this.descriptors) {
            Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("name", descriptor.getName());
            parameterMap.put("description", descriptor.getDescription());
            parameterMap.put("optional", descriptor.isOptional());
            parameterList.add(parameterMap);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("parameters", parameterList);

        return model;
    }

    public static MultipartFormParametersSnippet multipartFormParameters(
            MultipartParameterDescriptor... descriptors) {
        return new MultipartFormParametersSnippet(Arrays.asList(descriptors));
    }

    public static MultipartFormParametersSnippet multipartFormParameters(
            List<MultipartParameterDescriptor> descriptors) {
        return new MultipartFormParametersSnippet(descriptors);
    }

    public static MultipartParameterDescriptor multipartParameter(String name) {
        return new MultipartParameterDescriptor(name);
    }

    public static class MultipartParameterDescriptor {
        private final String name;
        private String description;
        private boolean optional = false;

        public MultipartParameterDescriptor(String name) {
            this.name = name;
        }

        public MultipartParameterDescriptor description(String description) {
            this.description = description;
            return this;
        }

        public MultipartParameterDescriptor optional() {
            this.optional = true;
            return this;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isOptional() {
            return optional;
        }
    }
}
