package com.agentcommerce.domain.identity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserProfileServiceTest {

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final UserProfileService service = new UserProfileService(repository);

    @Test
    void reportsOptimisticLockConflictWhenProfileVersionIsStale() {
        UUID userId = UUID.randomUUID();
        UpdateCustomerProfileRequest request = profileUpdate(false, 0L, 3L);
        when(repository.updateProfile(userId, request)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(userId, request))
            .isInstanceOf(UserConflictException.class)
            .hasMessageContaining("reload");
    }

    @Test
    void requiresPositiveLimitWhenEnablingAutoOrder() {
        UUID userId = UUID.randomUUID();
        UpdateCustomerProfileRequest request = profileUpdate(true, null, 0L);

        assertThatThrownBy(() -> service.updateProfile(userId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive maximum");
        verify(repository, never()).updateProfile(userId, request);
    }

    @Test
    void rejectsDeviceTokenAlreadyOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        RegisterDeviceTokenRequest request = new RegisterDeviceTokenRequest("token-123", "IOS");
        when(repository.findDeviceTokenOwner(request.token()))
            .thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.registerDeviceToken(userId, request))
            .isInstanceOf(UserConflictException.class)
            .hasMessageContaining("another user");
        verify(repository, never()).registerDeviceToken(userId, request);
    }

    @Test
    void hidesOwnershipWhenDeletingAnotherUsersAddress() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(repository.deleteAddress(userId, addressId)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteAddress(userId, addressId))
            .isInstanceOf(UserResourceNotFoundException.class)
            .hasMessageContaining("Address");
    }

    @Test
    void normalizesSupportedConsentAndRejectsUnknownTypes() {
        UUID userId = UUID.randomUUID();
        RecordConsentRequest request = new RecordConsentRequest(true, "2026-09");

        service.recordConsent(userId, " location_tracking ", request);
        verify(repository).recordConsent(userId, "LOCATION_TRACKING", request);

        assertThatThrownBy(() -> service.recordConsent(userId, "marketing", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported");
    }

    private UpdateCustomerProfileRequest profileUpdate(
        Boolean autoOrderEnabled,
        Long autoOrderMaxAmountMinor,
        long expectedVersion
    ) {
        return new UpdateCustomerProfileRequest(
            null,
            "Ada Customer",
            "en-US",
            "America/Los_Angeles",
            List.of("VEGETARIAN"),
            List.of("PEANUTS"),
            "APPROVAL_REQUIRED",
            autoOrderEnabled,
            autoOrderMaxAmountMinor,
            new BigDecimal("18.00"),
            expectedVersion
        );
    }
}