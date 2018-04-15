package pl.lodz.p.it.wks.wksrecruiter.config.security;

public class SecurityConstants {

    static final String SECRET = "tTmAyENEKNMWBmWKKYnRGyywp5X0gpVUoZHnHGOw2drxl9UtB6KCBlXCziKMzOF";
    static final long EXPIRATION_TIME = 864_000_000;
    static final String TOKEN_PREFIX = "Token ";
    static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    static final String ROLES_CLAIM_NAME = "roles";

    public static final String LOGOUT_URL = "/logout";
    public static final String LOGIN_URL = "/login";
    public static final String REGISTER_URL = "/accounts/register";

}
