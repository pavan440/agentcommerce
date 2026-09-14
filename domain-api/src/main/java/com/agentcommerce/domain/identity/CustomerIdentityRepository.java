package com.agentcommerce.domain.identity;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class CustomerIdentityRepository {

    private final JdbcClient jdbcClient;
    private final UserProfileRepository userProfileRepository;

    CustomerIdentityRepository(JdbcClient jdbcClient, UserProfileRepository userProfileRepository) {
        this.jdbcClient = jdbcClient;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    CustomerProfileResponse findOrCreateCustomer(IdentityClaims claims) {
        String identityKey = claims.issuer() + "|" + claims.subject();
        jdbcClient.sql("SELECT pg_advisory_xact_lock(hashtextextended(:identityKey, 0))")
            .param("identityKey", identityKey)
            .query((resultSet, rowNumber) -> Boolean.TRUE)
            .single();

        Optional<UUID> existingUserId = jdbcClient.sql("""
                SELECT user_id
                FROM user_identities
                WHERE issuer = :issuer AND subject = :subject
                """)
            .param("issuer", claims.issuer())
            .param("subject", claims.subject())
            .query(UUID.class)
            .optional();

        UUID userId = existingUserId.orElseGet(() -> createCustomer(claims));
        updateLastLogin(userId, claims);
        return userProfileRepository.findProfile(userId).orElseThrow();
    }

    private UUID createCustomer(IdentityClaims claims) {
        UUID userId = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO users (id, email, phone, status)
                VALUES (:id, :email, :phone, 'ACTIVE')
                """)
            .param("id", userId)
            .param("email", claims.email())
            .param("phone", claims.phone())
            .update();

        jdbcClient.sql("""
                INSERT INTO user_identities
                    (id, user_id, issuer, subject, provider, email_verified, last_login_at)
                VALUES
                    (:id, :userId, :issuer, :subject, :provider, :emailVerified, now())
                """)
            .param("id", UUID.randomUUID())
            .param("userId", userId)
            .param("issuer", claims.issuer())
            .param("subject", claims.subject())
            .param("provider", providerName(claims.issuer()))
            .param("emailVerified", claims.emailVerified())
            .update();

        jdbcClient.sql("INSERT INTO user_roles (user_id, role) VALUES (:userId, 'CUSTOMER')")
            .param("userId", userId)
            .update();

        jdbcClient.sql("""
                INSERT INTO customer_profiles (user_id, display_name, locale, timezone)
                VALUES (:userId, :displayName, :locale, 'UTC')
                """)
            .param("userId", userId)
            .param("displayName", displayName(claims))
            .param("locale", claims.locale())
            .update();

        return userId;
    }

    private void updateLastLogin(UUID userId, IdentityClaims claims) {
        jdbcClient.sql("""
                UPDATE user_identities
                SET last_login_at = now(), email_verified = :emailVerified
                WHERE user_id = :userId AND issuer = :issuer AND subject = :subject
                """)
            .param("emailVerified", claims.emailVerified())
            .param("userId", userId)
            .param("issuer", claims.issuer())
            .param("subject", claims.subject())
            .update();
    }

    private String providerName(String issuer) {
        try {
            return Optional.ofNullable(URI.create(issuer).getHost()).orElse(issuer);
        } catch (IllegalArgumentException exception) {
            return issuer;
        }
    }

    private String displayName(IdentityClaims claims) {
        if (claims.displayName() != null && !claims.displayName().isBlank()) {
            return claims.displayName();
        }
        if (claims.email() != null && !claims.email().isBlank()) {
            int separator = claims.email().indexOf('@');
            return claims.email().substring(0, separator > 0 ? separator : claims.email().length());
        }
        return "Customer";
    }
}
