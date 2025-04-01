package com.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.entity.RestBean;
import com.example.entity.dto.Client;
import com.example.service.ClientService;
import com.example.utils.Const;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用于对请求头中Jwt令牌进行校验的工具，为当前请求添加用户验证信息
 * 并将用户的ID存放在请求对象属性中，方便后续使用
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    JwtUtils utils;

    @Resource
    ClientService clientService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String url = request.getRequestURI();
        if(url.startsWith("/monitor")){
            if(!url.endsWith("/register")){
                Client client = clientService.findClientByToken(authorization);
                System.out.println(client);
                if(client == null){
                    response.setStatus(401);
                    response.getWriter().write(RestBean.failure(401,"weizhuce").asJsonString());
                    return;
                }else{
                    request.setAttribute(Const.ATTR_Client, client);  //设置的是Client client = new Client(id , "未命名主机" , token , new Date());
                }
            }
        }else {
            DecodedJWT jwt = utils.resolveJwt(authorization);
            if(jwt != null) {
                UserDetails user = utils.toUser(jwt);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(Const.ATTR_USER_ID, utils.toId(jwt));
            }
        }

        filterChain.doFilter(request, response);
    }
}
