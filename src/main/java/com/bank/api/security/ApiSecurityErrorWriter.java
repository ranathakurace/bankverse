package com.bank.api.security;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

final class ApiSecurityErrorWriter {
    private ApiSecurityErrorWriter() { }
    static void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        // Keep this filter-level response independent of Spring MVC's ObjectMapper configuration.
        // A plain ObjectMapper cannot serialize LocalDateTime without the Java Time module,
        // which can otherwise leave a partially written response when authentication fails.
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status);
        error.put("errorCode", code);
        error.put("message", message);
        error.put("path", request.getRequestURI());
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), error);
    }
}
