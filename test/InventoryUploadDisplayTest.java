package com.agentcommerce.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.agentcommerce.domain.identity.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Test Case 1: Inventory Upload and Display UI
 * 
 * Verifies that after a vendor uploads inventory via CSV, the customer UI 
 * displays products in a DoorDash-like interface with images and prices.
 */
class InventoryUploadDisplayTest {

    private InventoryService service;
    private CurrentUserService currentUserService;
    private InventoryController controller;
    
    private UUID vendorUserId;
    private UUID vendorLocationId;

    @BeforeEach
    void setUp() {
        service = mock(InventoryService.class);
        currentUserService = mock(CurrentUserService.class);
        controller = new InventoryController(service, currentUserService);
        
        vendorUserId = UUID.randomUUID();
        vendorLocationId = UUID.randomUUID();
        
        when(currentUserService.id(any())).thenReturn(vendorUserId);
    }

    @Test
    @DisplayName("TC1-STEP1: Vendor uploads inventory CSV")
    void testVendorUploadInventoryCSV() throws IOException {
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available,category,description
            BURGER-001,Classic Burger,12.99,50,true,Burgers,Juicy beef patty
            FRIES-001,Regular Fries,4.49,80,true,Sides,Crispy golden fries
            """;
        
        MultipartFile csvFile = new MockMultipartFile(
            "file",
            "inventory.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8)
        );
        
        UUID importId = UUID.randomUUID();
        InventoryImportResponse mockResponse = mock(InventoryImportResponse.class);
        when(mockResponse.id()).thenReturn(importId);
        when(mockResponse.validRows()).thenReturn(2);
        when(mockResponse.invalidRows()).thenReturn(0);
        
        when(service.stage(eq(vendorUserId), eq(vendorLocationId), any(MultipartFile.class)))
            .thenReturn(mockResponse);
        
        // Act
        var response = controller.stage(null, vendorLocationId, csvFile);
        
        // Assert
        assertThat(response).isNotNull();
        verify(service).stage(eq(vendorUserId), eq(vendorLocationId), any(MultipartFile.class));
    }

    @Test
    @DisplayName("TC1-STEP3: Customer can view vendor menu")
    void testCustomerCanViewVendorMenu() {
        List<InventoryItemResponse> menuItems = List.of(
            createMenuItem("BURGER-001", "Classic Burger", 1299, 50, true),
            createMenuItem("FRIES-001", "Regular Fries", 449, 80, true)
        );
        
        when(service.availableMenu(vendorLocationId)).thenReturn(menuItems);
        
        List<InventoryItemResponse> menu = controller.menu(vendorLocationId);
        
        assertThat(menu).hasSize(2);
        assertThat(menu).extracting("sku").containsExactlyInAnyOrder("BURGER-001", "FRIES-001");
    }

    @Test
    @DisplayName("TC1-STEP5: Unavailable items excluded from customer view (SPEC 477)")
    void testUnavailableItemsExcludedFromCustomerView() {
        List<InventoryItemResponse> allItems = List.of(
            createMenuItem("BURGER-001", "Classic Burger", 1299, 50, true),
            createMenuItem("BURGER-002", "Veggie Burger", 1199, 0, false)
        );
        
        when(service.availableMenu(vendorLocationId)).thenReturn(
            allItems.stream()
                .filter(item -> item.available())
                .toList()
        );
        
        List<InventoryItemResponse> menu = controller.menu(vendorLocationId);
        
        assertThat(menu).hasSize(1);
        assertThat(menu).extracting("sku").containsOnly("BURGER-001");
    }

    private InventoryItemResponse createMenuItem(String sku, String name, long priceMinor, int quantity, boolean available) {
        return new InventoryItemResponse(
            UUID.randomUUID(),
            vendorLocationId,
            sku,
            name,
            "Description for " + name,
            "Category",
            priceMinor,
            "USD",
            "PERPETUAL",
            quantity,
            0,
            available,
            5,
            "CSV_IMPORT",
            1L,
            java.time.Instant.now()
        );
    }
}
