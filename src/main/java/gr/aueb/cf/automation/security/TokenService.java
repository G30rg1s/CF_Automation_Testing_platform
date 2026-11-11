package gr.aueb.cf.automation.security;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final long ttl;
    public TokenService(JwtEncoder encoder, @Value("${app.jwt.ttl-seconds}") long ttl) {
        this.encoder = encoder; this.ttl = ttl;
    }
    public long expiresIn() { return ttl; }
    public String create(Long id) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("coding-factory-automation")
            .subject(id.toString()).issuedAt(now).expiresAt(now.plusSeconds(ttl)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
