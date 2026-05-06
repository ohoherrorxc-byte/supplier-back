package srm.portal.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import srm.portal.service.TokenService;

import java.io.IOException;

@Component
public class AuthFilter implements Filter {

    @Autowired
    private TokenService tokenService;

    private static final String[] EXCLUDED_PATHS = {
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/change-password",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // 放行不需要验证的路径
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 获取 token
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证 token
        TokenService.TokenInfo tokenInfo = tokenService.validateToken(token);
        if (tokenInfo == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"message\":\"未登录或登录已过期\"}");
            return;
        }

        // 将用户信息放入请求属性，供后续 controller 使用
        req.setAttribute("userId", tokenInfo.userId);
        req.setAttribute("userName", tokenInfo.userName);
        req.setAttribute("isAdmin", tokenInfo.isAdmin);
        req.setAttribute("isSupplierClient", tokenInfo.isSupplierClient);

        chain.doFilter(request, response);
    }
}