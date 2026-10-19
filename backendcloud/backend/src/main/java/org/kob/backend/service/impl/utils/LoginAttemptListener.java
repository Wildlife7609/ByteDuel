package org.kob.backend.service.impl.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

// Listens to Spring Security's login events and updates the failed-login counts (see LoginAttemptLimiter)
// An unknown username is reported as a bad password (Spring Security does not distinguish them by default), so it is counted too
@Component
public class LoginAttemptListener {
    @Autowired
    private LoginAttemptLimiter loginAttemptLimiter;

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptLimiter.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptLimiter.recordSuccess(event.getAuthentication().getName());
    }
}
