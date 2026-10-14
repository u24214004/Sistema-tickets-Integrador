package pe.edu.utp.tickets.auth;
import java.time.Instant;
import java.util.Locale;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.tickets.api.ApiException;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final CurrentUser current;
    private final String dummyHash;
    public AuthController(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder, CurrentUser current) {
        this.users=users; this.passwords=passwords; this.encoder=encoder; this.current=current;
        this.dummyHash=passwords.encode("nonexistent-account");
    }
    public record LoginRequest(@NotBlank @Email @Size(max=150) String email,
                               @NotBlank @Size(max=72) String password) {}
    public record UserView(Long id, String name, String email, Role role, Long companyId, String companyName) {
        public static UserView of(AppUser user) {
            return new UserView(user.getId(),user.getName(),user.getEmail(),user.getRole(),
                user.getCompany()==null ? null : user.getCompany().getId(),
                user.getCompany()==null ? null : user.getCompany().getName());
        }
    }
    public record LoginResponse(String token, Instant expiresAt, UserView user) {}
    @PostMapping("/login") @Transactional(readOnly=true)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user=users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        boolean valid=passwords.matches(request.password(), user==null ? dummyHash : user.getPasswordHash());
        if (user==null || !valid) throw new ApiException(HttpStatus.UNAUTHORIZED,"Credenciales incorrectas");
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("techconsulting-tickets").subject(user.getId().toString())
            .issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("role",user.getRole().name()).build();
        var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims));
        return new LoginResponse(token.getTokenValue(),claims.getExpiresAt(),UserView.of(user));
    }
    @GetMapping("/me") @Transactional(readOnly=true)
    public UserView me(@AuthenticationPrincipal Jwt jwt) { return UserView.of(current.resolve(jwt)); }
}
