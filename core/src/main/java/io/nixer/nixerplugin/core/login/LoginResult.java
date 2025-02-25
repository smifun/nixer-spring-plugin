package io.nixer.nixerplugin.core.login;

import java.util.function.Consumer;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.springframework.util.Assert;

/**
 * Represents result of authentication
 */
public class LoginResult {
    private final Status status;
    private final LoginFailureType reason;

    private LoginResult(final Status status, final LoginFailureType reason) {
        Assert.notNull(status, "Status must not be null");
        this.status = status;

        this.reason = reason;
    }

    public static LoginResult success() {
        return new LoginResult(Status.SUCCESS, null);
    }

    public static LoginResult failure(LoginFailureType reason) {
        Assert.notNull(reason, "Reason must not be null");

        return new LoginResult(Status.FAILURE, reason);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public LoginFailureType getFailureType() {
        return reason;
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }

    public LoginResult onSuccess(Consumer<LoginResult> consumer) {
        if (isSuccess()) {
            consumer.accept(this);
        }
        return this;
    }

    public LoginResult onFailure(Consumer<LoginResult> consumer) {
        if (!isSuccess()) {
            consumer.accept(this);
        }
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final LoginResult that = (LoginResult) o;
        return status == that.status &&
                reason == that.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(status, reason);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("reason", reason)
                .toString();
    }
}



