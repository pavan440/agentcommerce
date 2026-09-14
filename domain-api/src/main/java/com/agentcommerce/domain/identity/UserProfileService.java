package com.agentcommerce.domain.identity;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private static final List<String> CONSENT_TYPES = List.of(
        "LOCATION_TRACKING",
        "AGENT_PERSONALIZATION"
    );

    private final UserProfileRepository repository;

    UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public CustomerProfileResponse getProfile(UUID userId) {
        return repository.findProfile(userId)
            .orElseThrow(() -> new UserResourceNotFoundException("User profile was not found"));
    }

    public CustomerProfileResponse updateProfile(UUID userId, UpdateCustomerProfileRequest request) {
        validateProfile(request);
        return repository.updateProfile(userId, request)
            .orElseThrow(() -> new UserConflictException("Profile changed; reload the resource and retry"));
    }

    public List<CustomerAddressResponse> getAddresses(UUID userId) {
        return repository.findAddresses(userId);
    }

    public CustomerAddressResponse createAddress(UUID userId, CreateCustomerAddressRequest request) {
        return repository.createAddress(userId, request);
    }

    public void deleteAddress(UUID userId, UUID addressId) {
        if (!repository.deleteAddress(userId, addressId)) {
            throw new UserResourceNotFoundException("Address was not found");
        }
    }

    public List<DeviceTokenResponse> getDeviceTokens(UUID userId) {
        return repository.findDeviceTokens(userId);
    }

    public DeviceTokenResponse registerDeviceToken(UUID userId, RegisterDeviceTokenRequest request) {
        repository.findDeviceTokenOwner(request.token()).ifPresent(ownerId -> {
            if (!ownerId.equals(userId)) {
                throw new UserConflictException("Device token is already registered to another user");
            }
        });
        return repository.registerDeviceToken(userId, request);
    }

    public void deleteDeviceToken(UUID userId, UUID tokenId) {
        if (!repository.deleteDeviceToken(userId, tokenId)) {
            throw new UserResourceNotFoundException("Device token was not found");
        }
    }

    public List<EmergencyContactResponse> getEmergencyContacts(UUID userId) {
        return repository.findEmergencyContacts(userId);
    }

    public EmergencyContactResponse createEmergencyContact(
        UUID userId,
        CreateEmergencyContactRequest request
    ) {
        return repository.createEmergencyContact(userId, request);
    }

    public void deleteEmergencyContact(UUID userId, UUID contactId) {
        if (!repository.deleteEmergencyContact(userId, contactId)) {
            throw new UserResourceNotFoundException("Emergency contact was not found");
        }
    }

    public List<ConsentResponse> getCurrentConsents(UUID userId) {
        return repository.findCurrentConsents(userId);
    }

    public ConsentResponse recordConsent(
        UUID userId,
        String consentType,
        RecordConsentRequest request
    ) {
        String normalizedType = consentType.trim().toUpperCase(java.util.Locale.ROOT);
        if (!CONSENT_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported consent type");
        }
        return repository.recordConsent(userId, normalizedType, request);
    }

    private void validateProfile(UpdateCustomerProfileRequest request) {
        if (request.displayName() != null && request.displayName().isBlank()) {
            throw new IllegalArgumentException("Display name cannot be blank");
        }
        if (request.locale() != null && request.locale().isBlank()) {
            throw new IllegalArgumentException("Locale cannot be blank");
        }
        if (request.timezone() != null && request.timezone().isBlank()) {
            throw new IllegalArgumentException("Timezone cannot be blank");
        }
        if (Boolean.TRUE.equals(request.autoOrderEnabled())
            && (request.autoOrderMaxAmountMinor() == null
                || request.autoOrderMaxAmountMinor() == 0)) {
            throw new IllegalArgumentException("Auto-order requires a positive maximum amount");
        }
    }
}
