package com.visualbuilder.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IP 工具类测试")
class IpUtilsTest {

    @Test
    @DisplayName("无代理时取 remoteAddr")
    void directConnection() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        assertEquals("192.168.1.100", IpUtils.getClientIp(request));
    }

    @Test
    @DisplayName("X-Forwarded-For 单层代理")
    void singleProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50");

        assertEquals("203.0.113.50", IpUtils.getClientIp(request));
    }

    @Test
    @DisplayName("X-Forwarded-For 多级代理取第一个")
    void multiProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.50, 150.172.238.178");

        assertEquals("203.0.113.50", IpUtils.getClientIp(request));
    }

    @Test
    @DisplayName("X-Real-IP 头")
    void realIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.25");

        assertEquals("198.51.100.25", IpUtils.getClientIp(request));
    }

    @Test
    @DisplayName("unknown 值被跳过")
    void unknownHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        request.addHeader("X-Forwarded-For", "unknown");

        assertEquals("192.168.1.1", IpUtils.getClientIp(request));
    }

    @Test
    @DisplayName("null request 返回空字符串")
    void nullRequest() {
        assertEquals("", IpUtils.getClientIp((HttpServletRequest) null));
    }

    @Test
    @DisplayName("无上下文时 getClientIp() 返回空字符串")
    void noRequestContext() {
        assertEquals("", IpUtils.getClientIp());
    }
}
