package com.agentcommerce.domain.inventory;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.agentcommerce.domain.identity.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/vendor-locations/{locationId}")
public class InventoryController {
    private final InventoryService service; private final CurrentUserService users;
    public InventoryController(InventoryService service,CurrentUserService users){this.service=service;this.users=users;}
    @GetMapping("/inventory") public List<InventoryItemResponse> inventory(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID locationId){return service.vendorInventory(users.id(jwt),locationId);}
    @PutMapping("/inventory/items") public InventoryItemResponse upsert(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID locationId,@Valid @RequestBody UpsertInventoryItemRequest request){return service.upsert(users.id(jwt),locationId,request);}
    @PostMapping(value="/inventory-imports",consumes="multipart/form-data") public ResponseEntity<InventoryImportResponse> stage(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID locationId,@RequestPart("file") MultipartFile file){var value=service.stage(users.id(jwt),locationId,file);return ResponseEntity.created(URI.create("/v1/vendor-locations/"+locationId+"/inventory-imports/"+value.id())).body(value);}
    @GetMapping("/inventory-imports/{importId}") public InventoryImportResponse getImport(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID locationId,@PathVariable UUID importId){return service.getImport(users.id(jwt),locationId,importId);}
    @PostMapping("/inventory-imports/{importId}/commit") public InventoryImportResponse commit(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID locationId,@PathVariable UUID importId){return service.commit(users.id(jwt),locationId,importId);}
    @GetMapping("/menu") public List<InventoryItemResponse> menu(@PathVariable UUID locationId){return service.availableMenu(locationId);}
}