package com.itheima.fve.fve.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.common.R;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.web.servlet.ServletComponentScan; // 通常放在启动类上
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录检查过滤器
 * 1. 定义白名单，放行不需要登录的请求（登录页、静态资源、前台、公共接口等）
 * 2. 检查 Session 中是否存在登录标识 ("employee" 或 "user")
 * 3. 如果已登录，将用户 ID 存入 BaseContext 的 ThreadLocal 中，并放行
 * 4. 如果未登录：
 *    - 判断是 API 请求还是页面请求
 *    - API 请求：返回 NOTLOGIN 的 JSON
 *    - 页面请求：重定向到登录页面
 * 5. 在请求处理完毕后，清理 BaseContext 中的 ThreadLocal
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
// @ServletComponentScan // 推荐放在 Spring Boot 主启动类上
public class LoginCheckFilter implements Filter {

    // 路径匹配器
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // 定义白名单 (***关键：确保登录页资源路径正确***)
    private static final String[] WHITE_LIST_URLS = new String[]{
            "/employee/login",          // 后台登录 API (这是后端接口路径, 需要登录才能调)
            "/employee/logout",         // 后台登出 API
            "/user/login",              // 前台登录 API
            "/user/sendMsg",            // 前台发短信 API

            // --- 允许访问登录页面及其所需资源
            "/backend/page/login/login.html",
            "/backend/plugins/**",
            "/backend/styles/**",
            "/backend/images/**",
            "/backend/js/**",
            "/backend/api/**",
            "/favicon.ico",

            // --- 允许访问前台页面及其资源 ---
            "/front/**",

            // --- 允许访问公共资源 ---
            "/common/**",

            // --- API文档相关 ---
            "/doc.html",
            "/webjars/**",
            "/swagger-resources",
            "/v2/api-docs",
            "/user/register"

    };

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        long threadId = Thread.currentThread().getId(); // 获取线程ID

        try { // *** 添加 try ***
            String requestURI = request.getRequestURI();
            log.info("线程 {} - 启动过滤器，拦截到请求：{}...", threadId, requestURI);

            // 检查白名单
            boolean check = check(WHITE_LIST_URLS, requestURI);
            log.debug("线程 {} - 白名单检查结果 (放行={}): {}", threadId, check, check); // 使用 debug 级别

            if (check) {
                log.info("线程 {} - 请求 [{}] 在白名单，放行...", threadId, requestURI);
                filterChain.doFilter(request, response);
                return; // 白名单请求处理完毕
            }

            // 检查后台登录 Session
            Object employeeSession = request.getSession().getAttribute("employee");
            log.debug("线程 {} - 检查 Session 'employee'. Value: {}, Type: {}", threadId, employeeSession, (employeeSession != null ? employeeSession.getClass().getName() : "null"));

            if (employeeSession != null && employeeSession instanceof Long) {
                Long empId = (Long) employeeSession;
                log.info("线程 {} - 后台用户已登录，ID: {}", threadId, empId);
                BaseContext.setCurrentId(empId); // 设置 BaseContext
                log.debug("线程 {} - BaseContext 已设置 ID: {}", threadId, empId); // 确认设置
                filterChain.doFilter(request, response); // 放行
                return; // 处理完毕
            } else {
                log.debug("线程 {} - 后台 Session 'employee' 未找到或类型不匹配.", threadId);
                if (employeeSession != null) { // 如果存在但类型不对，记录错误
                    log.error("线程 {} - Session 'employee' 类型错误! 应为 Long, 实为: {}", threadId, employeeSession.getClass().getName());
                    // 可以选择返回错误或阻止，这里先不处理，让后续逻辑判断是否未登录
                }
            }

            // 检查前台登录 Session (如果需要)
            Object userSession = request.getSession().getAttribute("user");
            log.debug("线程 {} - 检查 Session 'user'. Value: {}, Type: {}", threadId, userSession, (userSession != null ? userSession.getClass().getName() : "null"));
            if (userSession != null && userSession instanceof Long) {
                Long userId = (Long) userSession;
                log.info("线程 {} - 前台用户已登录，ID: {}", threadId, userId);
                BaseContext.setCurrentId(userId);
                log.debug("线程 {} - BaseContext 已设置 ID: {}", threadId, userId);
                filterChain.doFilter(request, response);
                return;
            } else {
                log.debug("线程 {} - 前台 Session 'user' 未找到或类型不匹配.", threadId);
                if (userSession != null) {
                    log.error("线程 {} - Session 'user' 类型错误! 应为 Long, 实为: {}", threadId, userSession.getClass().getName());
                }
            }

            // 执行到此 = 未登录且不在白名单
            log.warn("线程 {} - 用户未登录，访问被拦截：{}", threadId, requestURI);

            String acceptHeader = request.getHeader("Accept");
            // 简单判断：如果 Accept 头包含 application/json，认为是 API 请求
            // 这个判断可能不完全准确，但对于常见的前后端分离场景通常有效
            boolean isApiRequest = acceptHeader != null && acceptHeader.toLowerCase().contains("application/json");
            log.info("线程 {} - Accept Header: [{}],判定为 API 请求? {}", threadId, acceptHeader, isApiRequest);

            if (isApiRequest) {
                // API 请求，返回 JSON，让前端 request.js 处理跳转
                log.info("线程 {} - API请求未登录，返回 NOTLOGIN JSON", threadId);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
            } else {
                // 页面或其它请求，服务器端重定向到登录页
                String contextPath = request.getContextPath(); // 获取应用上下文路径
                String redirectUrl = contextPath + "/backend/page/login/login.html";
                log.info("线程 {} - 页面/其他请求未登录，执行服务器端重定向到: {}", threadId, redirectUrl);
                response.sendRedirect(redirectUrl); // *** 使用 sendRedirect ***
            }

            return; // 处理完毕

        } finally {
            log.info("线程 {} - 请求处理完毕，清理 BaseContext", threadId);
            BaseContext.removeCurrentId();
        }
    }

    /**
     * 路径匹配检查 (保持不变)
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                log.debug("线程 {} - URI [{}] 匹配白名单模式: [{}]", Thread.currentThread().getId(), requestURI, url);
                return true;
            }
        }
        return false;
    }
}