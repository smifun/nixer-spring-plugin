package eu.xword.nixer.nixerplugin.captcha;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import eu.xword.nixer.nixerplugin.blocking.policies.BadCaptchaException;
import eu.xword.nixer.nixerplugin.captcha.error.RecaptchaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.util.Assert;

public class CaptchaChecker implements UserDetailsChecker {

    // TODO convert to Blocking policy
    //TODO split config of captcha blocking policy from captch itself
    private static final String LOGIN_ACTION = "login";

    @Autowired
    private HttpServletRequest request;

    private CaptchaServiceFactory captchaServiceFactory;

    private String captchaParam;

    private CaptchaService captchaService;

    private CaptchaStrategy captchaStrategy = ALWAYS;

    public CaptchaChecker(final CaptchaServiceFactory captchaServiceFactory, final RecaptchaProperties recaptchaProperties) {
        Assert.notNull(captchaServiceFactory, "CaptchaServiceFactory must not be null");
        this.captchaServiceFactory = captchaServiceFactory;

        Assert.notNull(recaptchaProperties, "RecaptchaProperties must not be null");
        this.captchaParam = recaptchaProperties.getParam();
    }

    @PostConstruct
    public void postInit() {
        this.captchaService = captchaServiceFactory.createCaptchaService(LOGIN_ACTION);
    }

    @Override
    public void check(final UserDetails toCheck) {
        if (applies()) {
            final String captchaValue = request.getParameter(captchaParam);

            try {
                captchaService.processResponse(captchaValue);
            } catch (RecaptchaException e) {
                throw new BadCaptchaException("Invalid captcha", e);
            }
        }
    }

    public boolean applies() {
        return captchaStrategy.applies();
    }

    public void setCaptchaStrategy(final CaptchaStrategy captchaStrategy) {
        Assert.notNull(captchaStrategy, "CaptchaStrategy must not be null");

        this.captchaStrategy = captchaStrategy;
    }

    public static CaptchaStrategy ALWAYS = () -> Boolean.TRUE;
    public static CaptchaStrategy NEVER = () -> Boolean.FALSE;

}
