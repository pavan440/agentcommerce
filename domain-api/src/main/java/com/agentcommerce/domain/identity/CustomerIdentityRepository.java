package com.agentcommerce.domain.identity;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class CustomerIdentityRepository {

    private final JdbcClient jdbcClient;

    CustomerIdentityRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    CustomerProfileResponse findOrCreateCustomer(IdentityClaims claims) {
        String identityKey = claims.issuer() + "|" + claims.subject();
        jdbcClient.sql("SELECT pg_advisory_xact_lock(hashtextextended(:identityKey, 0))")
            .param("identityKey", identityKey)
            .query(Long.class)
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
        return loadProfile(userId);
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

    private CustomerProfileResponse loadProfile(UUID userId) {
        ProfileRow profile = jdbcClient.sql("""
                SELECT u.id, u.email, u.phone, p.display_name, p.locale, p.timezone
                FROM users u
                JOIN customer_profiles p ON p.user_id = u.id
                WHERE u.id = :userId
                """)
            .param("userId", userId)
            .query((resultSet, rowNumber) -> new ProfileRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("display_name"),
                resultSet.getString("locale"),
                resultSet.getString("timezone")
            ))
            .single();

        Set<String> roles = new LinkedHashSet<>(jdbcClient.sql("""
                SELECT role
                FROM user_roles
                WHERE user_id = :userId
                ORDER BY role
                """)
            .param("userId", userId)
            .query(String.class)
            .list());

        return new CustomerProfileResponse(
            profile.id(),
            profile.email(),
            profile.phone(),
            profile.displayName(),
            profile.locale(),
            profile.timezone(),
            Set.copyOf(roles)
        );
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

    private record ProfileRow(
        UUID id,
        String email,
        String phone,
        String displayName,
        String locale,
        String timezone
    ) {
    }
}

