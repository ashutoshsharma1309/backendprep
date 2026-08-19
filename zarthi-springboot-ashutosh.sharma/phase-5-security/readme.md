# Phase 5 — Securing the Application
### Prerequisite: Phase 4 complete. Branch: `git checkout -b phase-5-yourname`

**Catalog services (all Senior-tier — pair with a mentor throughout):**
API Security, Protect REST APIs, Secure Application Secrets.

---

## Module 1 — Spring Security Fundamentals

### 1.1 Add the Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

The moment this is on the classpath, **every endpoint requires a login** by
default. Run the app and hit `GET /books` — you'll get a `401` and a
generated password in the startup logs. That's Spring Security's default
lockdown, deliberately conservative until you configure it.

### 1.2 A Basic Security Configuration

Create `project/src/main/java/com/example/library/config/SecurityConfig.java`:

```java
package com.example.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // fine for a stateless JSON API; explained in Module 2
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/books/lookup/**").permitAll()
                .requestMatchers("GET", "/books/**").permitAll()
                .requestMatchers("DELETE", "/books/**").hasRole("LIBRARIAN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

`PasswordEncoder` (BCrypt) is here so you **never** compare or store
plaintext passwords — always hash on the way in, and compare hashes, never
strings.

**Your task this phase:** try deleting a book without authentication (should
be `403` or `401` depending on config), then confirm `GET /books` still
works without auth (it's `permitAll()`).

---

## Module 2 — JWT Authentication

### 2.1 Why Tokens, Not Sessions

REST APIs are meant to be stateless — no server-side session to remember who
you are between requests. A **JWT** (JSON Web Token) carries identity
*inside* the request itself: the client sends
`Authorization: Bearer <token>` on every call, and the server verifies the
token's signature instead of looking up a session.

**Important: a JWT's payload is Base64-encoded, not encrypted.** Anyone can
decode and read it — the signature only proves it wasn't *tampered with*,
not that it's secret. Never put a password or other sensitive data in a
JWT claim.

### 2.2 Issuing a Token

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

```java
package com.example.library.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expiryMillis = 3600_000; // 1 hour

    public JwtService(@Value("${library.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String issueToken(String username, String role) {
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusMillis(expiryMillis)))
            .signWith(key)
            .compact();
    }

    public io.jsonwebtoken.Claims validateAndParse(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
    }
}
```

`library.jwt.secret` comes from configuration — **Module 4 covers keeping
this out of source control**, but for now add a placeholder to
`application-dev.properties`:
```properties
library.jwt.secret=dev-only-placeholder-change-in-real-config
```

**Your task this phase:** add a `POST /auth/login` endpoint accepting a
username/password, checking it (a hardcoded in-memory user map is fine for
training), and returning `{"token": "..."}` via `JwtService.issueToken()`.

### 2.3 Validating Tokens on Every Request

You need a filter that runs before your controllers, checks for a valid
`Authorization` header, and tells Spring Security who the caller is. This is
genuinely fiddly the first time — work through it with your mentor rather
than alone. The shape:

```java
package com.example.library.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws IOException, jakarta.servlet.ServletException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.validateAndParse(header.substring(7));
                String role = claims.get("role", String.class);
                var auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // Invalid/expired token: leave unauthenticated, let Spring Security's
                // normal 401/403 handling take over downstream — don't throw here.
            }
        }
        chain.doFilter(request, response);
    }
}
```

Wire it into `SecurityConfig` with `.addFilterBefore(jwtAuthFilter,
UsernamePasswordAuthenticationFilter.class)`.

**Your task this phase:** log in via `/auth/login`, take the returned token,
and confirm `DELETE /books/{id}` now works with
`Authorization: Bearer <token>` when the token has role `LIBRARIAN`, and
returns `403` for a token with a different role.

---

## Module 3 — Protecting REST APIs

### 3.1 Basic Rate Limiting

A simple, dependency-free approach for training (production systems
typically use a library like Bucket4j or a gateway-level limiter — Phase 9
covers gateway-level protection):

```java
package com.example.library.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRateLimiter {

    private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    private final int limitPerMinute = 60;

    public boolean allow(String clientKey) {
        AtomicInteger count = counts.computeIfAbsent(clientKey, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= limitPerMinute;
        // Training-grade only: real systems need a sliding/rolling window and
        // a scheduled reset, not just an ever-growing counter.
    }
}
```

**Your task this phase:** wrap this in a filter or interceptor that returns
`429 Too Many Requests` when `allow()` returns false, keyed by client IP or
authenticated username.

### 3.2 CORS — Do It Properly, Not by Disabling It

```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000") // the real frontend origin, not "*"
                .allowedMethods("GET", "POST", "PUT", "DELETE");
        }
    };
}
```

**Try it:** confirm a request from an allowed origin succeeds and one from
a different origin is blocked by the browser (test with a small fetch from
a different local port, or just reason through why `allowedOrigins("*")`
combined with credentials would be a real security hole).

---

## Module 4 — Managing Secrets

You added `library.jwt.secret` as a plaintext placeholder in Module 2 — fix
that now.

**Never do this** (what you currently have):
```properties
# application-dev.properties, committed to Git
library.jwt.secret=dev-only-placeholder-change-in-real-config
```

**Do this instead** — reference an environment variable, with no real
secret value anywhere in a committed file:
```properties
library.jwt.secret=${LIBRARY_JWT_SECRET}
```
```bash
export LIBRARY_JWT_SECRET=$(openssl rand -base64 32)
mvn spring-boot:run
```

**Your task this phase:**
1. Remove the plaintext secret from every properties file.
2. Confirm the app fails to start cleanly (not with a confusing unrelated
   error) if `LIBRARY_JWT_SECRET` isn't set — that's a *good* failure mode,
   catching a misconfiguration immediately instead of silently running
   insecurely.
3. Write, in your PR description, the steps you'd take if this secret were
   accidentally pushed to a shared repo (hint: it's not just "delete the
   commit" — the old value must be treated as compromised and rotated).

---

## Phase 5 Completion Checklist

- [ ] `SecurityConfig` in place; unauthenticated requests to protected endpoints correctly rejected
- [ ] `/auth/login` issues a JWT; protected endpoints validate it via a filter
- [ ] Role-based restriction proven (`LIBRARIAN` role required for delete)
- [ ] Rate limiting returns `429` beyond the configured threshold
- [ ] CORS configured to a specific origin, not wildcarded
- [ ] JWT secret fully externalized, app fails fast if missing, rotation steps documented
- [ ] PR opened, mentor-reviewed (all three services here are Senior-tier), CI green

**Next:** [Phase 6 — Core Business Logic & Reliability](../phase-6-business-logic-reliability/README.md)
