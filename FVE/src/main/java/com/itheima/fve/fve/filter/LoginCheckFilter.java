package com.itheima.fve.fve.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
@ServletComponentScan
public class LoginCheckFilter implements Filter {


    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1、获取本次请求的URI
        String requestURI = request.getRequestURI();
        log.info("启动过滤器，拦截到请求：{}...",request.getRequestURI());
        //2、判断本次请求是否需要处理
        //定义不需要处理的路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login",
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"

        };

        boolean check = check(urls,requestURI);
        //3、如果不需要处理，则直接放行
        if(check){
            log.info("本次请求不需要处理：{}...",requestURI);
            filterChain.doFilter(request,response);
            return;
        }
        //4-1、判断登录状态，如果已登录,则直接放行
        if(request.getSession().getAttribute("employee")!=null){
            log.info("用户已登录，用户ID为：{}...",request.getSession().getAttribute("employee"));


            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request,response);
            return;
        }

        //4-2、判断移动端登录状态，如果已登录,则直接放行
        if(request.getSession().getAttribute("user")!=null){
            log.info("用户已登录，用户ID为：{}...",request.getSession().getAttribute("user"));


            Long empId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request,response);
            return;
        }

        //5、如果未登录则返回未登录结果，通过输出流方式向客户端页面响应数据
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));

        return;
    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls,requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for(String url : urls){
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match)return true;
        }
        return false;

    }
}
