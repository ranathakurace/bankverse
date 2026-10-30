package com.bank.api.security;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component public class RestAccessDeniedHandler implements AccessDeniedHandler {
    @Override public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException { ApiSecurityErrorWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED", "You are not authorized to perform this operation."); }
}
