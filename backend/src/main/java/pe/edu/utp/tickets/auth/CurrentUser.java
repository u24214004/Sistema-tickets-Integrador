package pe.edu.utp.tickets.auth;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import pe.edu.utp.tickets.api.ApiException;
@Service
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users) { this.users=users; }
    public AppUser resolve(Jwt jwt) {
        try {
            return users.findById(Long.parseLong(jwt.getSubject()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Sesión inválida"));
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,"Sesión inválida");
        }
    }
}
