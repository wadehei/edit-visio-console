package com.visualbuilder.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.local.base-path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件目录为可访问的静态资源
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }

    /**
     * 上传文件的响应头加固：/uploads/** 无需登录即可匿名访问（SecurityConfig 白名单），
     * 该目录内容来自用户上传，必须假设其中可能存在恶意载荷：
     * <ul>
     *   <li>{@code X-Content-Type-Options: nosniff} —— 阻止浏览器嗅探把文本当 HTML 执行；</li>
     *   <li>{@code Content-Security-Policy: sandbox} —— 即使用户把恶意 HTML/SVG 伪装成
     *       合法扩展名存进来，浏览器也会在禁脚本的沙箱里渲染，无法触达应用同源的令牌。</li>
     * </ul>
     */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> uploadSecurityHeadersFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Content-Security-Policy",
                        "sandbox; default-src 'none'; img-src 'self' data:; media-src 'self'");
                chain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/uploads/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}
