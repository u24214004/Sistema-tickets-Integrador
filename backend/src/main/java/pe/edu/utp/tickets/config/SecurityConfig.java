package pe.edu.utp.tickets.config;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean SecretKey jwtKey(@Value("${app.jwt-secret}") String configured,
                          @Value("${app.demo.enabled:false}") boolean demo) {
        byte[] bytes=configured.getBytes(StandardCharsets.UTF_8);
        if (bytes.length==0 && demo) { bytes=new byte[32]; new SecureRandom().nextBytes(bytes); }
        if (bytes.length<32) throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key) {
        var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("techconsulting-tickets"));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        var authorities=new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role"); authorities.setAuthorityPrefix("ROLE_");
        var converter=new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http.cors(cors -> {}).csrf(csrf -> csrf.disable())
            // Bearer tokens are sent explicitly in Authorization, never in an authentication cookie.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/health").permitAll()
                .requestMatchers("/api/companies").hasRole("INTERNO")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((request,response,error) -> {
                    response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"Inicia sesión para continuar\"}");
                }))
            .exceptionHandling(errors -> errors.accessDeniedHandler((request,response,error) -> {
                response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"No tienes permiso para realizar esta operación\"}");
            })).build();
    }
    @Bean CorsConfigurationSource cors(@Value("${app.cors-origin}") String origin) {
        var config=new CorsConfiguration(); config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET","POST","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        config.setMaxAge(3600L);
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**",config);
        return source;
    }
}
