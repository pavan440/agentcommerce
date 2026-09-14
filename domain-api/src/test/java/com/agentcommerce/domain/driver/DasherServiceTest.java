package com.agentcommerce.domain.driver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DasherServiceTest {

    private final DasherRepository repository = mock(DasherRepository.class);
    private final DasherService service = new DasherService(repository);

    @Test
    void rejectsDuplicateOnboarding() {
        UUID userId = UUID.randomUUID();
        CreateDasherProfileRequest request = motorVehicleRequest();
        when(repository.findProfile(userId)).thenReturn(Optional.of(profile(userId, "PENDING_VERIFICATION", 0)));

        assertThatThrownBy(() -> service.createProfile(userId, request))
            .isInstanceOf(DasherConflictException.class)
            .hasMessageContaining("already exists");
        verify(repository, never()).createProfile(userId, request);
    }

    @Test
    void requiresCompleteMotorVehicleDetails() {
        UUID userId = UUID.randomUUID();
        CreateDasherProfileRequest request = new CreateDasherProfileRequest(
            "CAR", null, "Camry", "Silver", "ABC123", "D1234567", 3
        );
        when(repository.findProfile(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProfile(userId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Motor vehicles");
        verify(repository, never()).createProfile(userId, request);
    }

    @Test
    void rejectsStaleProfileUpdate() {
        UUID userId = UUID.randomUUID();
        UpdateDasherProfileRequest request = new UpdateDasherProfileRequest(
            null, null, null, "Blue", null, null, 4, 0
        );
        when(repository.findProfile(userId)).thenReturn(Optional.of(profile(userId, "PENDING_VERIFICATION", 1)));
        when(repository.updateProfile(userId, request)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(userId, request))
            .isInstanceOf(DasherConflictException.class)
            .hasMessageContaining("reload");
    }

    @Test
    void blocksUnverifiedDasherFromGoingOnline() {
        UUID userId = UUID.randomUUID();
        UpdateDasherAvailabilityRequest request = new UpdateDasherAvailabilityRequest(true, 0);
        when(repository.findProfile(userId)).thenReturn(Optional.of(profile(userId, "PENDING_VERIFICATION", 0)));

        assertThatThrownBy(() -> service.updateAvailability(userId, request))
            .isInstanceOf(DasherAccessDeniedException.class)
            .hasMessageContaining("verified active");
        verify(repository, never()).updateAvailability(userId, request);
    }

    @Test
    void requiresOperatingZoneBeforeGoingOnline() {
        UUID userId = UUID.randomUUID();
        UpdateDasherAvailabilityRequest request = new UpdateDasherAvailabilityRequest(true, 2);
        when(repository.findProfile(userId)).thenReturn(Optional.of(profile(userId, "ACTIVE", 2)));
        when(repository.hasOperatingZone(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateAvailability(userId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("operating zone");
        verify(repository, never()).updateAvailability(userId, request);
    }

    @Test
    void requiresOperatorRoleAndApprovedCheckForActivation() {
        UUID operatorId = UUID.randomUUID();
        UUID dasherId = UUID.randomUUID();
        ReviewDasherRequest approved = new ReviewDasherRequest("ACTIVE", "APPROVED", 0);
        when(repository.hasRole(operatorId, "OPERATOR")).thenReturn(false);

        assertThatThrownBy(() -> service.reviewProfile(operatorId, dasherId, approved))
            .isInstanceOf(DasherAccessDeniedException.class)
            .hasMessageContaining("Operator");

        when(repository.hasRole(operatorId, "OPERATOR")).thenReturn(true);
        ReviewDasherRequest rejected = new ReviewDasherRequest("ACTIVE", "REJECTED", 0);
        assertThatThrownBy(() -> service.reviewProfile(operatorId, dasherId, rejected))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("approved background");
        verify(repository, never()).reviewProfile(any(), any());
    }

    @Test
    void rejectsDegenerateZoneAndOwnershipSafeDelete() {
        UUID userId = UUID.randomUUID();
        DriverZonePoint point = new DriverZonePoint(47.61, -122.33);
        CreateDasherOperatingZoneRequest request =
            new CreateDasherOperatingZoneRequest("Downtown", List.of(point, point, point));
        when(repository.findProfile(userId)).thenReturn(Optional.of(profile(userId, "ACTIVE", 0)));

        assertThatThrownBy(() -> service.createOperatingZone(userId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("distinct");

        UUID zoneId = UUID.randomUUID();
        when(repository.findProfile(userId)).thenReturn(Optional.of(profile(userId, "ACTIVE", 0)));
        when(repository.deleteOperatingZone(userId, zoneId)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteOperatingZone(userId, zoneId))
            .isInstanceOf(DasherResourceNotFoundException.class);
    }

    private CreateDasherProfileRequest motorVehicleRequest() {
        return new CreateDasherProfileRequest(
            "CAR", "Toyota", "Camry", "Silver", "ABC123", "D1234567", 3
        );
    }

    private DasherProfileResponse profile(UUID userId, String status, long version) {
        return new DasherProfileResponse(
            userId,
            status,
            "CAR",
            "Toyota",
            "Camry",
            "Silver",
            "ABC123",
            "4567",
            "ACTIVE".equals(status) ? "APPROVED" : "PENDING",
            false,
            3,
            new BigDecimal("5.00"),
            0,
            version,
            Instant.now(),
            Instant.now()
        );
    }
}