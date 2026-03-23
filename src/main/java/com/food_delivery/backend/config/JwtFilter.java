package com.food_delivery.backend.config;

import com.food_delivery.backend.service.impl.CustomUserDetailsService;
import com.food_delivery.backend.util.JwtUtil;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.extractEmail(token);

                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                        if (jwtUtil.extractTokenVersion(token) != user.getTokenVersion()) {
                            SecurityContextHolder.clearContext();
                            chain.doFilter(request, response);
                            return;
                        }

                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        if (!userDetails.isEnabled()) {
                            SecurityContextHolder.clearContext();
                            chain.doFilter(request, response);
                            return;
                        }

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
