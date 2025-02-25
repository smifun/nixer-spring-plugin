package io.nixer.nixerplugin.core.stigma.token.validation;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import io.nixer.nixerplugin.core.stigma.token.StigmaTokenConstants;
import io.nixer.nixerplugin.core.stigma.token.StigmaTokenConstants;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;

import static io.nixer.nixerplugin.core.stigma.token.validation.ValidationStatus.EXPIRED;
import static io.nixer.nixerplugin.core.stigma.token.validation.ValidationStatus.INVALID_PAYLOAD;
import static io.nixer.nixerplugin.core.stigma.token.validation.ValidationStatus.MISSING_STIGMA;
import static io.nixer.nixerplugin.core.stigma.token.validation.ValidationStatus.PAYLOAD_PARSING_ERROR;
import static java.lang.String.format;

/**
 * Verifies if the passed JWT is parsable and it's payload contains a valid Stigma Token.
 *
 * Created on 2019-05-29.
 *
 * @author gcwiak
 */
public class StigmaTokenPayloadValidator implements JwtValidator {

    @Nonnull
    private final Supplier<Instant> nowSource;

    @Nonnull
    private final Duration tokenLifetime;

    public StigmaTokenPayloadValidator(@Nonnull final Supplier<Instant> nowSource, @Nonnull final Duration tokenLifetime) {
        Assert.notNull(nowSource, "nowSource must not be null");
        this.nowSource = nowSource;

        Assert.notNull(tokenLifetime, "Duration must not be null");
        this.tokenLifetime = tokenLifetime;
    }

    @Override
    public ValidationResult validate(@Nonnull final JWT jwt) {
        Assert.notNull(jwt, "JWT must not be null");

        final JWTClaimsSet result;
        try {
            result = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            return ValidationResult.invalid(ValidationStatus.PAYLOAD_PARSING_ERROR, format("Payload parsing error: [%s]", e.getMessage()));
        }

        return validatePayload(result);
    }

    private ValidationResult validatePayload(final JWTClaimsSet claims) {

        // TODO consider accumulating violations instead failing fast at first encountered one

        final Instant now = nowSource.get();

        final Object stigmaValue = claims.getClaim(StigmaTokenConstants.STIGMA_VALUE_FIELD_NAME);
        if (stigmaValue == null || StringUtils.isEmpty(stigmaValue.toString())) {
            return ValidationResult.invalid(ValidationStatus.MISSING_STIGMA, "Missing stigma value");
        }

        final String validStigma = stigmaValue.toString();

        if (!StigmaTokenConstants.SUBJECT.equals(claims.getSubject())) {
            return ValidationResult.invalid(ValidationStatus.INVALID_PAYLOAD, format("Invalid subject: [%s]", claims.getSubject()), validStigma);
        }

        final Date issueTime = claims.getIssueTime();
        if (issueTime == null) {
            return ValidationResult.invalid(ValidationStatus.INVALID_PAYLOAD, "Missing issued-at", validStigma);
        }

        final Instant expirationTime = issueTime.toInstant().plus(tokenLifetime);

        if (now.isAfter(expirationTime)) {
            return ValidationResult.invalid(ValidationStatus.EXPIRED,
                    format("Expired token. Issued at: [%s], validation time: [%s], token lifetime: [%s] ", issueTime, now, tokenLifetime),
                    validStigma);
        }

        return ValidationResult.valid(validStigma);
    }
}
