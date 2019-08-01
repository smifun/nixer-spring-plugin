package eu.xword.nixer.nixerplugin.blocking.policies;

import org.springframework.security.authentication.BadCredentialsException;

public class BadCaptchaException extends BadCredentialsException {
    public BadCaptchaException(final String msg) {
        super(msg);
    }

    public BadCaptchaException(final String msg, final Throwable t) {
        super(msg, t);
    }
}
