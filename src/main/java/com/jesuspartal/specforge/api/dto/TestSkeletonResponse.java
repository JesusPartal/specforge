package com.jesuspartal.specforge.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated JUnit test skeleton")
public record TestSkeletonResponse(
        @Schema(description = "Test class name") String className,
        @Schema(description = "Full Java source code") String javaCode
) {}