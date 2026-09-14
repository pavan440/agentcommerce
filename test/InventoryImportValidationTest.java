package com.agentcommerce.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.agentcommerce.domain.vendor.VendorRepository;

/**
 * Test cases for Inventory Management functionality as defined in SPEC.md Section 7.
 * 
 * Test Coverage:
 * - TC-INV-002: CSV Import Validation
 * - TC-INV-003: CSV Schema Validation  
 * - TC-INV-005: Checkout with Insufficient Inventory
 */
class InventoryImportValidationTest {

    private final InventoryRepository inventoryRepo = mock(InventoryRepository.class);
    private final VendorRepository vendorRepo = mock(VendorRepository.class);
    private final InventoryImportParser parser = new InventoryImportParser();
    private final InventoryService service = new InventoryService(inventoryRepo, vendorRepo, parser);

    @Test
    void tc_inv_002_validatesCsvWithMixedValidAndInvalidRows() {
        // SPEC Section 7.3: CSV Import must validate each row and report errors
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available,operation
            SKU-001,Valid Item,9.99,10,true,upsert
            SKU-002,,5.00,5,true,upsert
            SKU-003,Negative Price,-1.00,10,true,upsert
            SKU-004,Three Decimals,10.999,5,true,upsert
            SKU-005,Duplicate Later,8.99,20,true,upsert
            SKU-005,Duplicate SKU,7.99,15,true,upsert
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        // Verify validation identifies errors
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrorCount()).isGreaterThan(0);
        
        // Missing name error
        assertThat(result.getErrors())
            .anySatisfy(error -> 
                assertThat(error.getMessage()).containsIgnoringCase("name")
            );
        
        // Negative price error
        assertThat(result.getErrors())
            .anySatisfy(error -> 
                assertThat(error.getMessage()).containsIgnoringCase("price")
                    .or(hasMessageContaining("negative"))
            );
        
        // Duplicate SKU error
        assertThat(result.getErrors())
            .anySatisfy(error -> 
                assertThat(error.getMessage()).containsIgnoringCase("duplicate")
                    .or(hasMessageContaining("unique"))
            );
        
        // Valid rows should be identified separately
        assertThat(result.getValidRowCount()).isGreaterThan(0);
        
        // Import should not be committed until errors resolved
        verify(inventoryRepo, never()).commitImport(locationId, result.getImportId());
    }

    @Test
    void tc_inv_003_rejectsNegativePrice() {
        // SPEC Section 7.3: price must be non-negative decimal
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available
            BURGER-001,Burger,-5.99,10,true
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .anySatisfy(error -> 
                assertThat(error.getRowNumber()).isEqualTo(2)
            );
    }

    @Test
    void tc_inv_003_rejectsPriceWithMoreThanTwoDecimalPlaces() {
        // SPEC Section 7.3: price must have at most two fractional digits for USD
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available
            SHAKE-001,Shake,5.999,10,true
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .anySatisfy(error -> 
                assertThat(error.getMessage()).containsIgnoringCase("decimal")
                    .or(hasMessageContaining("fractional"))
                    .or(hasMessageContaining("precision"))
            );
    }

    @Test
    void tc_inv_003_rejectsDuplicateSkusWithinSameImport() {
        // SPEC Section 7.3: sku must be unique within a vendor location
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available
            FRIES-001,Fries,4.99,50,true
            FRIES-001,Extra Fries,5.99,30,true
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .anySatisfy(error -> 
                assertThat(error.getMessage()).containsIgnoringCase("duplicate")
                    .or(hasMessageContaining("unique"))
            );
    }

    @Test
    void tc_inv_003_acceptsValidCsvWithoutErrors() {
        // SPEC Section 7.3: Valid CSV should pass validation
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available,category,operation
            BURGER-001,Classic Burger,12.99,50,true,Burgers,upsert
            FRIES-001,Regular Fries,4.49,80,true,Sides,upsert
            SHAKE-VAN,Vanilla Shake,5.99,0,false,Drinks,update
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getValidRowCount()).isEqualTo(3);
    }

    @Test
    void tc_inv_002_requiresVendorConfirmationBeforeCommit() {
        // SPEC Section 7.3: Import must not partially apply until vendor reviews and confirms
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available
            ITEM-001,Test Item,9.99,10,true
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        // Before confirmation, no commit should occur
        verify(inventoryRepo, never()).commitImport(locationId, result.getImportId());
        
        // After explicit confirmation
        service.confirmImport(userId, locationId, result.getImportId());
        
        verify(inventoryRepo).commitImport(locationId, result.getImportId());
    }

    @Test
    void tc_inv_002_allowsCommitOnlyWithErrorsExcluded() {
        // SPEC Section 7.3: Imports containing errors cannot be committed unless invalid rows excluded
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available
            VALID-001,Valid,9.99,10,true
            INVALID-001,,5.00,5,true
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        assertThat(result.hasErrors()).isTrue();
        
        // Attempting to commit with errors should fail
        assertThatThrownBy(() -> service.commitImport(userId, locationId, result.getImportId()))
            .isInstanceOf(InventoryImportException.class)
            .hasMessageContaining("errors");
        
        // Exclude error rows and retry
        service.excludeErrorRows(userId, locationId, result.getImportId());
        
        // Now commit should succeed
        service.commitImport(userId, locationId, result.getImportId());
        
        verify(inventoryRepo).commitImport(locationId, result.getImportId());
    }

    @Test
    void tc_inv_002_createsAuditRecordOnCommit() {
        // SPEC Section 7.3: Every committed import receives an ID and audit record
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available
            AUDIT-001,Audit Test,15.99,25,true
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        service.confirmImport(userId, locationId, result.getImportId());
        service.commitImport(userId, locationId, result.getImportId());
        
        // Verify audit record created
        verify(inventoryRepo).createAuditRecord(
            locationId, 
            result.getImportId(), 
            userId, 
            "COMMIT"
        );
    }

    @Test
    void tc_inv_003_warnsOnUnknownColumnsButDoesNotFail() {
        // SPEC Section 7.3: Unknown columns generate warnings but do not fail the import
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available,unknown_column,another_unknown
            WARN-001,Warning Test,7.99,10,true,data1,data2
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        // Should have warnings but no errors
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getWarningCount()).isGreaterThan(0);
    }

    @Test
    void tc_inv_003_handlesUtf8Encoding() {
        // SPEC Section 7.3: System must support .csv files encoded as UTF-8
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        // UTF-8 content with special characters
        String csvContent = """
            sku,name,price,quantity_on_hand,is_available
            CAFE-001,Café Mélangé,6.99,20,true
            TACO-001,Tacos al Pastor,8.99,15,true
            """;
        
        InputStream csvStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(vendorRepo.canManageLocation(userId, locationId)).thenReturn(true);
        
        InventoryImportValidationResult result = service.validateImport(userId, locationId, csvStream);
        
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getValidRowCount()).isEqualTo(2);
    }

    @Test
    void tc_inv_005_preventsCheckoutWithInsufficientInventory() {
        // SPEC Section 7.4: Item with insufficient available quantity cannot be added to cart
        UUID locationId = UUID.randomUUID();
        String sku = "LIMITED-001";
        int availableQuantity = 5;
        int requestedQuantity = 6;
        
        when(inventoryRepo.getAvailableQuantity(locationId, sku))
            .thenReturn(availableQuantity);
        
        assertThatThrownBy(() -> service.validateAvailability(locationId, sku, requestedQuantity))
            .isInstanceOf(InventoryInsufficientException.class)
            .hasMessageContaining("insufficient")
            .hasMessageContaining(String.valueOf(availableQuantity));
        
        verify(inventoryRepo).getAvailableQuantity(locationId, sku);
    }

    @Test
    void tc_inv_005_allowsCheckoutWithSufficientInventory() {
        // SPEC Section 7.4: Items with sufficient inventory can be reserved
        UUID locationId = UUID.randomUUID();
        String sku = "AVAILABLE-001";
        int availableQuantity = 10;
        int requestedQuantity = 5;
        
        when(inventoryRepo.getAvailableQuantity(locationId, sku))
            .thenReturn(availableQuantity);
        when(inventoryRepo.reserveInventory(locationId, sku, requestedQuantity))
            .thenReturn(true);
        
        boolean reserved = service.reserveInventory(locationId, sku, requestedQuantity);
        
        assertThat(reserved).isTrue();
        verify(inventoryRepo).reserveInventory(locationId, sku, requestedQuantity);
    }
}
