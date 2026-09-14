package com.agentcommerce.domain.vendor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class VendorServiceTest {

    private final VendorRepository repository = mock(VendorRepository.class);
    private final VendorService service = new VendorService(repository);

    @Test
    void rejectsLocationCreationWithoutOwnerOrAdminMembership() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        CreateVendorLocationRequest request = locationRequest();

        when(repository.canCreateLocations(userId, vendorId)).thenReturn(false);

        assertThatThrownBy(() -> service.createLocation(userId, vendorId, request))
            .isInstanceOf(VendorAccessDeniedException.class)
            .hasMessageContaining("owner or administrator");

        verify(repository, never()).createVendorLocation(vendorId, request);
    }

    @Test
    void createsLocationForAuthorizedOwner() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        CreateVendorLocationRequest request = locationRequest();

        when(repository.canCreateLocations(userId, vendorId)).thenReturn(true);

        service.createLocation(userId, vendorId, request);

        verify(repository).createVendorLocation(vendorId, request);
    }

    @Test
    void reportsOptimisticLockConflictWhenSettingsVersionIsStale() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UpdateCommerceSettingsRequest request = commerceSettingsRequest(0);

        when(repository.canManageLocation(userId, locationId)).thenReturn(true);
        when(repository.updateCommerceSettings(locationId, request)).thenReturn(false);

        assertThatThrownBy(() -> service.updateCommerceSettings(userId, locationId, request))
            .isInstanceOf(VendorConflictException.class)
            .hasMessageContaining("reload");

        verify(repository).updateCommerceSettings(locationId, request);
    }

    @Test
    void rejectsInvalidPreparationRangeBeforeUpdating() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UpdateCommerceSettingsRequest request = new UpdateCommerceSettingsRequest(
            true,
            false,
            null,
            null,
            0,
            true,
            true,
            false,
            "TABLET",
            20,
            30,
            60,
            0L,
            0L,
            "PROVIDER",
            false,
            null,
            0
        );

        when(repository.canManageLocation(userId, locationId)).thenReturn(true);

        assertThatThrownBy(() -> service.updateCommerceSettings(userId, locationId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Minimum preparation time");

        verify(repository, never()).updateCommerceSettings(locationId, request);
    }

    @Test
    void rejectsBlankCommunitySearch() {
        assertThatThrownBy(() -> service.getLocationsInCommunity("  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Community code");
    }

    private CreateVendorLocationRequest locationRequest() {
        return new CreateVendorLocationRequest(
            "SEA-CAPITOL-HILL",
            "Pine Street",
            "pine-street",
            "100 Pine Street",
            null,
            "Seattle",
            "WA",
            "98101",
            "US",
            "100 Pine Street, Seattle, WA 98101",
            47.6101,
            -122.3344,
            "America/Los_Angeles",
            null,
            null,
            null,
            null,
            "store@example.com",
            null,
            null,
            null,
            null
        );
    }

    private UpdateCommerceSettingsRequest commerceSettingsRequest(long expectedVersion) {
        return new UpdateCommerceSettingsRequest(
            true,
            false,
            Instant.parse("2026-09-14T02:00:00Z"),
            "Kitchen maintenance",
            5,
            true,
            true,
            false,
            "TABLET",
            25,
            10,
            60,
            1000L,
            100L,
            "PROVIDER",
            false,
            null,
            expectedVersion
        );
    }
}
