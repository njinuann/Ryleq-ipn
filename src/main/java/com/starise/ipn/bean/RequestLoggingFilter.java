package com.starise.ipn.bean;

import com.starise.ipn.service.IpnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

@WebFilter("/*")
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest(httpServletRequest);

        String requestBody = cachedBodyHttpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String headers = Collections.list(httpServletRequest.getHeaderNames()).stream()
                .map(headerName -> headerName + ": " + httpServletRequest.getHeader(headerName))
                .collect(Collectors.joining(System.lineSeparator()));

        logger.info("Request URL::=>{}",  httpServletRequest.getRequestURL());
        logger.info("Request Method::=>{}",  httpServletRequest.getMethod());
        logger.info("Request Headers::=>{}",  headers);
        logger.info("Request Body::=>{}",  requestBody);

        chain.doFilter(cachedBodyHttpServletRequest, response);
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}

