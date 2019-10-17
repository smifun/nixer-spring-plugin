package eu.xword.nixer.nixerplugin.filter;

import javax.servlet.http.HttpServletRequest;

import eu.xword.nixer.nixerplugin.registry.UserAgentOverLoginThresholdRegistry;
import eu.xword.nixer.nixerplugin.useragent.UserAgentTokenizer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static eu.xword.nixer.nixerplugin.filter.RequestAugmentation.USER_AGENT_FAILED_LOGIN_OVER_THRESHOLD;
import static org.springframework.http.HttpHeaders.USER_AGENT;

/**
 * Appends information if presented username is over threshold for failed login.
 */
@Component
public class UserAgentFailedLoginOverThresholdFilter extends MetadataFilter {

    private final UserAgentOverLoginThresholdRegistry userAgentOverLoginThresholdRegistry;

    public UserAgentFailedLoginOverThresholdFilter(UserAgentOverLoginThresholdRegistry userAgentOverLoginThresholdRegistry) {
        Assert.notNull(userAgentOverLoginThresholdRegistry, "UsernameOverLoginThresholdRegistry must not be null");
        this.userAgentOverLoginThresholdRegistry = userAgentOverLoginThresholdRegistry;
    }

    @Override
    protected void apply(final HttpServletRequest request) {
        final String userAgent = request.getHeader(USER_AGENT);
        final UserAgentTokenizer tokenizer = UserAgentTokenizer.sha1Tokenizer();
        final String userAgentToken = tokenizer.tokenize(userAgent);
        boolean overThreshold = userAgentToken != null && userAgentOverLoginThresholdRegistry.contains(userAgentToken);

        request.setAttribute(USER_AGENT_FAILED_LOGIN_OVER_THRESHOLD, overThreshold);
    }
}
