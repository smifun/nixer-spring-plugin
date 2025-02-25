package io.nixer.nixerplugin.core.detection.rules.threshold;

import io.nixer.nixerplugin.core.detection.rules.EventEmitter;
import io.nixer.nixerplugin.core.detection.events.IpFailedLoginOverThresholdEvent;
import io.nixer.nixerplugin.core.detection.events.UserAgentFailedLoginOverThresholdEvent;
import io.nixer.nixerplugin.core.login.LoginContext;
import io.nixer.nixerplugin.core.login.inmemory.LoginMetric;
import io.nixer.nixerplugin.core.detection.rules.EventEmitter;
import io.nixer.nixerplugin.core.login.LoginContext;
import io.nixer.nixerplugin.core.login.inmemory.LoginMetric;
import org.springframework.util.Assert;

/**
 * Rule that checks if number of login failures for useragent exceeds threshold and emits {@link IpFailedLoginOverThresholdEvent} event if it does.
 */
public class UserAgentLoginOverThresholdRule extends ThresholdAnomalyRule {

    private static final int THRESHOLD_VALUE = 10;

    private final LoginMetric failedLoginMetric;

    public UserAgentLoginOverThresholdRule(final LoginMetric failedLoginMetric) {
        super(THRESHOLD_VALUE);
        Assert.notNull(failedLoginMetric, "LoginMetric must not be null");
        this.failedLoginMetric = failedLoginMetric;
    }

    @Override
    public void execute(final LoginContext loginContext, final EventEmitter eventEmitter) {
        final String userAgent = loginContext.getUserAgentToken();
        final int failedLogin = failedLoginMetric.value(userAgent); //todo login Metric api not symmetrical.

        if (isOverThreshold(failedLogin)) {
            eventEmitter.accept(new UserAgentFailedLoginOverThresholdEvent(userAgent));
        }
    }

}
