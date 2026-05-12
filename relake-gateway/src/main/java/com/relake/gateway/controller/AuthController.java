package com.relake.gateway.controller;

import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.gateway.config.JwtUtil;
import com.relake.gateway.dto.LoginRequest;
import com.relake.gateway.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @Value("${relake.jwt.expiration:86400000}")
    private long expirationMs;

    @Value("${relake.jwt.admin-username:admin}")
    private String adminUsername;

    @Value("${relake.jwt.admin-password:admin}")
    private String adminPassword;

    @Value("${relake.jwt.admin-display-name:超级管理员}")
    private String adminDisplayName;

    /**
     * 登录 — 使用内置管理员账号（凭据可通过配置覆盖）
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return R.fail(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return R.fail(ResultCode.BAD_REQUEST, "密码不能为空");
        }

        if (!adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
            return R.fail(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(1L, request.getUsername());
        log.info("用户 {} 登录成功", request.getUsername());

        return R.ok(new LoginResponse(token, "Bearer", expirationMs / 1000,
                request.getUsername(), adminDisplayName));
    }
}
