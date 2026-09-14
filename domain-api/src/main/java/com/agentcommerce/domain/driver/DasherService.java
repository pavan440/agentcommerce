package com.agentcommerce.domain.driver;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class DasherService {

    private final DasherRepository repository;

    DasherService(DasherRepository repository) {
        this.repository = repository;
    }

    public DasherProfileResponse createProfile(UUID userId, CreateDasherProfileRequest request) {
        if (repository.findProfile(userId).isPresent()) {
            throw new DasherConflictException("Dasher onboarding already exists");
        }
        validateVehicle(
            request.vehicleType(),
            request.vehicleMake(),
            request.vehicleModel(),
            request.vehicleColor(),
            request.licensePlate(),
            request.licenseNumber()
        );
        return repository.createProfile(userId, request);
    }

    public DasherProfileResponse getProfile(UUID userId) {
        return repository.findProfile(userId)
            .orElseThrow(() -> new DasherResourceNotFoundException("Dasher profile was not found"));
    }

    public DasherProfileResponse updateProfile(UUID userId, UpdateDasherProfileRequest request) {
        DasherProfileResponse current = getProfile(userId);
        String vehicleType = valueOr(request.vehicleType(), current.vehicleType());
        boolean clearMotorVehicle = "BICYCLE".equals(vehicleType) || "FOOT".equals(vehicleType);
        validateVehicle(
            vehicleType,
            clearMotorVehicle ? null : valueOr(request.vehicleMake(), current.vehicleMake()),
            clearMotorVehicle ? null : valueOr(request.vehicleModel(), current.vehicleModel()),
            clearMotorVehicle ? null : valueOr(request.vehicleColor(), current.vehicleColor()),
            clearMotorVehicle ? null : valueOr(request.licensePlate(), current.licensePlate()),
            clearMotorVehicle ? null : valueOr(request.licenseNumber(), current.licenseNumberLast4())
        );
        return repository.updateProfile(userId, request)
            .orElseThrow(() -> new DasherConflictException("Dasher profile changed; reload and retry"));
    }

    public DasherProfileResponse updateAvailability(
        UUID userId,
        UpdateDasherAvailabilityRequest request
    ) {
        DasherProfileResponse profile = getProfile(userId);
        if (request.online()) {
            if (!"ACTIVE".equals(profile.status())
                || !"APPROVED".equals(profile.backgroundCheckStatus())) {
                throw new DasherAccessDeniedException("Only verified active dashers can go online");
            }
            if (!repository.hasOperatingZone(userId)) {
                throw new IllegalArgumentException("At least one operating zone is required to go online");
            }
        }
        return repository.updateAvailability(userId, request)
            .orElseThrow(() -> new DasherConflictException("Dasher profile changed; reload and retry"));
    }

    public DasherProfileResponse reviewProfile(
        UUID operatorId,
        UUID dasherId,
        ReviewDasherRequest request
    ) {
        if (!repository.hasRole(operatorId, "OPERATOR")) {
            throw new DasherAccessDeniedException("Operator role is required");
        }
        if ("ACTIVE".equals(request.status())
            && !"APPROVED".equals(request.backgroundCheckStatus())) {
            throw new IllegalArgumentException("An active dasher requires an approved background check");
        }
        if (repository.findProfile(dasherId).isEmpty()) {
            throw new DasherResourceNotFoundException("Dasher profile was not found");
        }
        return repository.reviewProfile(dasherId, request)
            .orElseThrow(() -> new DasherConflictException("Dasher profile changed; reload and retry"));
    }

    public List<DasherOperatingZoneResponse> getOperatingZones(UUID userId) {
        getProfile(userId);
        return repository.findOperatingZones(userId);
    }

    public DasherOperatingZoneResponse createOperatingZone(
        UUID userId,
        CreateDasherOperatingZoneRequest request
    ) {
        getProfile(userId);
        validateBoundary(request.boundary());
        return repository.createOperatingZone(
            userId,
            request.zoneName().trim(),
            toMultiPolygonWkt(request.boundary())
        );
    }

    public void deleteOperatingZone(UUID userId, UUID zoneId) {
        DasherProfileResponse profile = getProfile(userId);
        if (profile.online() && repository.countOperatingZones(userId) <= 1) {
            throw new IllegalArgumentException("An online dasher must keep at least one operating zone");
        }
        if (!repository.deleteOperatingZone(userId, zoneId)) {
            throw new DasherResourceNotFoundException("Operating zone was not found");
        }
    }

    private void validateVehicle(
        String vehicleType,
        String vehicleMake,
        String vehicleModel,
        String vehicleColor,
        String licensePlate,
        String licenseNumber
    ) {
        String normalizedType = vehicleType.toUpperCase(Locale.ROOT);
        if ("CAR".equals(normalizedType) || "SCOOTER".equals(normalizedType)) {
            if (isBlank(vehicleMake) || isBlank(vehicleModel) || isBlank(vehicleColor)
                || isBlank(licensePlate) || isBlank(licenseNumber)) {
                throw new IllegalArgumentException("Motor vehicles require make, model, color, plate, and license number");
            }
        }
    }

    private void validateBoundary(List<DriverZonePoint> boundary) {
        if (new LinkedHashSet<>(boundary).size() < 3) {
            throw new IllegalArgumentException("Operating zone requires three distinct boundary points");
        }
    }

    private String toMultiPolygonWkt(List<DriverZonePoint> boundary) {
        String points = boundary.stream()
            .map(point -> point.longitude() + " " + point.latitude())
            .collect(Collectors.joining(", "));
        DriverZonePoint first = boundary.getFirst();
        DriverZonePoint last = boundary.getLast();
        if (!first.equals(last)) {
            points += ", " + first.longitude() + " " + first.latitude();
        }
        return "MULTIPOLYGON (((" + points + ")))";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String valueOr(String provided, String current) {
        return provided != null ? provided : current;
    }
}