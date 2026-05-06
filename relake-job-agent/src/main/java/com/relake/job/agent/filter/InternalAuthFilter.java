package com.relake.job.agent.filter;

import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 内部认证过滤器 — 校验 X-Internal-Call 头
 * <p>
 * 拒绝所有不带 X-Internal-Call: true 的外部请求。
 */
@Slf4j
@RequiredArgsConstructor
public class InternalAuthFilter implements Filter {

    private final boolean enabled;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (!"true".equals(httpRequest.getHeader("X-Internal-Call"))) {
            log.warn("拒绝非内部请求: {} {} from {}",
                    httpRequest.getMethod(), httpRequest.getRequestURI(),
                    httpRequest.getRemoteAddr());

            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write(
                    objectMapper.writeValueAsString(
                            R.fail(ResultCode.FORBIDDEN, "仅允许内部服务调用")));
            return;
        }
        chain.doFilter(request, response);
    }
}
