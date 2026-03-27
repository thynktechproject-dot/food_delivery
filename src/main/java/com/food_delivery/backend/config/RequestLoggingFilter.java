package com.food_delivery.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        log.info("HTTP Request: {} {} | IP: {}",
                req.getMethod(),
                req.getRequestURI(),
                req.getRemoteAddr()
        );

        chain.doFilter(request, response);
    }
}