package cinema.web.security;

import cinema.controller.auth.PhienDangNhap;
import cinema.model.VaiTro;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class WebSecurity {
    @Bean
    SecurityFilterChain website(HttpSecurity http, WebAccounts accounts) throws Exception {
        http.authenticationProvider(accounts)
            .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/login", "/web/**", "/health", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasRole("STAFF")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login")
                .successHandler((req, res, auth) -> {
                    var user = (PhienDangNhap)auth.getPrincipal();
                    var session = req.getSession();
                    boolean admin = user.vaiTro() == VaiTro.ADMIN;
                    session.setAttribute("employeeId", user.nhanVien().maNV());
                    session.setAttribute("name", user.nhanVien().hoTen());
                    session.setAttribute("role", admin ? "ADMIN" : "STAFF");
                    res.sendRedirect(admin ? "/admin/showtimes" : "/staff/sales");
                })
                .failureHandler((req, res, ex) -> {
                    req.getSession().setAttribute("loginError", ex.getMessage());
                    res.sendRedirect("/login?error");
                }))
            .logout(logout -> logout.logoutSuccessUrl("/login"))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'")));
        return http.build();
    }
}
