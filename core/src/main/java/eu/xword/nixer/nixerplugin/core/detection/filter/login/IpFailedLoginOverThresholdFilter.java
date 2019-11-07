package eu.xword.nixer.nixerplugin.core.detection.filter.login;

import javax.servlet.http.HttpServletRequest;

import eu.xword.nixer.nixerplugin.core.detection.filter.MetadataFilter;
import eu.xword.nixer.nixerplugin.core.detection.registry.IpOverLoginThresholdRegistry;
import org.springframework.util.Assert;

import static eu.xword.nixer.nixerplugin.core.detection.filter.RequestMetadata.IP_FAILED_LOGIN_OVER_THRESHOLD;

/**
 * Appends information if request ip is over threshold for failed login.
 */
public class IpFailedLoginOverThresholdFilter extends MetadataFilter {

    private final IpOverLoginThresholdRegistry ipOverLoginThresholdRegistry;

    public IpFailedLoginOverThresholdFilter(final IpOverLoginThresholdRegistry ipOverLoginThresholdRegistry) {
        Assert.notNull(ipOverLoginThresholdRegistry, "IpOverLoginThresholdRegistry must not be null");
        this.ipOverLoginThresholdRegistry = ipOverLoginThresholdRegistry;
    }

    @Override
    protected void apply(final HttpServletRequest request) {
        final String ip = request.getRemoteAddr();
        final boolean isIpOverThreshold = ipOverLoginThresholdRegistry.contains(ip);
        request.setAttribute(IP_FAILED_LOGIN_OVER_THRESHOLD, isIpOverThreshold);
    }
}
