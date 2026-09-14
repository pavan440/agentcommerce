package com.agentcommerce.domain.driver;

import java.util.UUID;

import com.agentcommerce.domain.identity.CurrentUserService;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/operators/dashers")
class DasherOperatorController {

    private final CurrentUserService currentUserService;
    private final DasherService dasherService;

    DasherOperatorController(CurrentUserService currentUserService, DasherService dasherService) {
        this.currentUserService = currentUserService;
        this.dasherService = dasherService;
    }

    @PatchMapping("/{dasherId}/verification")
    DasherProfileResponse reviewProfile(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID dasherId,
        @Valid @RequestBody ReviewDasherRequest request
    ) {
        return dasherService.reviewProfile(currentUserService.id(jwt), dasherId, request);
    }
}