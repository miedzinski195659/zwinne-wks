package pl.lodz.p.it.wks.wksrecruiter.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.lodz.p.it.wks.wksrecruiter.collections.Account;
import pl.lodz.p.it.wks.wksrecruiter.repositories.AccountsRepository;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static pl.lodz.p.it.wks.wksrecruiter.config.security.SecurityConstants.*;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final AccountsRepository accountsRepository;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, AccountsRepository accountsRepository) {
        this.authenticationManager = authenticationManager;
        this.accountsRepository = accountsRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            Account account = new ObjectMapper().readValue(request.getInputStream(), Account.class);

            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    account.getLogin(),
                    account.getPassword(),
                    null
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        User user = (User) authResult.getPrincipal();
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .claim(ROLES_CLAIM_NAME, user.getAuthorities())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET.getBytes())
                .compact();
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        response.addHeader(AUTHORIZATION_HEADER_NAME, TOKEN_PREFIX + token);

        Optional<Account> accountOptional = accountsRepository.findByLogin(user.getUsername());
        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();
            account.setPassword(null);
            response.setContentType("application/json");
            response.getWriter().write(new Gson().toJson(account));
            response.getWriter().flush();
            response.getWriter().close();
        }
    }
}