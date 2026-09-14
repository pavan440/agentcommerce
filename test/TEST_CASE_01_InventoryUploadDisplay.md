# Test Case 1: Inventory Upload and Display UI

## Specification Reference
- SPEC Section 7.2: Form-Based Updates
- SPEC Section 7.3: CSV Import
- SPEC Section 6.1: Customer Ordering Flow (lines 272-274)
- MVP Goal: "Allow customers to discover local vendors and products" (SPEC line 27)

## Test Objective
Verify that after a vendor uploads inventory via CSV, the customer UI displays products in a DoorDash-like interface with:
1. Product images
2. Product names
3. Prices (showing base price + platform fee)
4. Vendor information
5. Availability status

## Pre-conditions
1. Vendor account exists and is authenticated
2. Customer account exists and is authenticated
3. Vendor location is configured and active
4. Platform fee configuration (X%) is set

## Test Steps

### Step 1: Vendor Uploads Inventory CSV
**Action:** Vendor uploads a CSV file with product data
**Input CSV:**
```csv
sku,name,price,quantity_on_hand,is_available,category,description,image_url
BURGER-001,Classic Burger,12.99,50,true,Burgers,"Juicy beef patty with lettuce and tomato",https://example.com/images/burger.jpg
FRIES-001,Regular Fries,4.49,80,true,Sides,"Crispy golden fries",https://example.com/images/fries.jpg
SHAKE-VAN,Vanilla Shake,5.99,30,true,Drinks,"Creamy vanilla milkshake",https://example.com/images/shake.jpg
SALAD-001,Caesar Salad,8.99,25,true,Salads,"Fresh romaine with caesar dressing",https://example.com/images/salad.jpg
BURGER-002,Veggie Burger,11.99,0,false,Burgers,"Plant-based patty option",https://example.com/images/veggie-burger.jpg
```

**Expected API Behavior:**
- POST `/v1/vendor-locations/{locationId}/inventory-imports` accepts the CSV
- Returns validation summary with row-level status
- Shows: 4 creates, 1 error (quantity_on_hand=0 with is_available=true conflict or warning)

**Test Code Location:** `test/InventoryUploadDisplayTest.java` - Method `testVendorUploadInventoryCSV()`

### Step 2: Vendor Confirms and Commits Import
**Action:** Vendor reviews validation and confirms import
**API Call:** POST `/v1/inventory-imports/{importId}/commit`

**Expected Result:**
- Import committed successfully
- Audit record created
- Inventory items available in system
- Event `inventory.import_committed` published

**Test Code Location:** `test/InventoryUploadDisplayTest.java` - Method `testCommitInventoryImport()`

### Step 3: Customer Discovers Vendor Products
**Action:** Customer searches or browses vendor catalog
**API Call:** GET `/v1/vendor-locations/{locationId}/menu` or GET `/v1/catalog/search`

**Expected Response Structure:**
```json
{
  "vendor": {
    "id": "uuid",
    "name": "Test Restaurant",
    "rating": 4.5,
    "deliveryTime": "25-35 min"
  },
  "products": [
    {
      "sku": "BURGER-001",
      "name": "Classic Burger",
      "description": "Juicy beef patty with lettuce and tomato",
      "image_url": "https://example.com/images/burger.jpg",
      "in_store_price": 12.99,
      "platform_fee": 1.95,
      "customer_price": 14.94,
      "is_available": true,
      "quantity_available": 50,
      "category": "Burgers"
    }
  ]
}
```

**Test Code Location:** `test/InventoryUploadDisplayTest.java` - Method `testCustomerCanViewVendorMenu()`

### Step 4: UI Renders DoorDash-like Display
**Action:** Customer portal renders product listing

**UI Requirements:**
1. **Card Layout:** Each product displayed in a card similar to DoorDash
   - Image on top or left side
   - Name prominently displayed
   - Price shown clearly
   - Availability indicator (e.g., "Available" or "Out of Stock")
   - Category badge/tag

2. **Price Display:**
   - Show final customer price (base + platform fee)
   - Optionally show breakdown: "$12.99 + $1.95 fee = $14.94"

3. **Image Handling:**
   - Load image from image_url
   - Show placeholder if image unavailable
   - Responsive image sizing

4. **Filtering/Sorting:**
   - Filter by category
   - Filter by availability (hide out of stock by default per SPEC 477)
   - Sort by price, popularity, etc.

**Test Code Location:** `test/InventoryUploadDisplayTest.java` - Method `testUIRendersProductCardsWithImagesAndPrices()`

### Step 5: Verify Out-of-Stock Items Excluded
**Action:** Customer browses active menu

**Expected Behavior (per SPEC 477):**
- BURGER-002 (Veggie Burger, quantity=0, is_available=false) should NOT appear in search results
- Only available items shown by default

**Test Code Location:** `test/InventoryUploadDisplayTest.java` - Method `testUnavailableItemsExcludedFromCustomerView()`

## Acceptance Criteria

✅ CSV upload validates all rows before committing
✅ Vendor can review and confirm import
✅ Committed inventory immediately available for customer viewing
✅ Customer UI displays products with images, names, and prices
✅ Price calculation shows base price + platform fee correctly
✅ Out-of-stock items excluded from customer catalog view (SPEC 477)
✅ UI layout resembles DoorDash (card-based, image-prominent)
✅ Product categories properly displayed and filterable
✅ Availability status clearly indicated

## Implementation Notes

### Backend Requirements
1. **InventoryImportParser** must validate CSV format (already exists)
2. **InventoryService** must handle stage/commit workflow (already exists)
3. **InventoryController** must expose menu endpoint (already exists at line 25)
4. **Pricing Service** must calculate platform fee based on configurable X%

### Frontend Requirements
1. **Customer Portal** (`/portal/customer/`) needs product listing component
2. **Product Card Component** with image, name, price, availability
3. **Category Filter** UI element
4. **Search Functionality** integration

### Test Data Setup
```java
@Test
public void testFullInventoryUploadAndDisplayFlow() {
    // 1. Create vendor location
    VendorLocation location = createVendorLocation();
    
    // 2. Upload CSV
    String csv = loadTestCsv("test-inventory-with-images.csv");
    InventoryImportResponse importResp = uploadInventory(location.getId(), csv);
    
    // 3. Validate import
    assertThat(importResp.getValidRows()).isEqualTo(4);
    assertThat(importResp.getErrorRows()).isEqualTo(1);
    
    // 4. Commit import
    InventoryImportResponse committed = commitImport(location.getId(), importResp.getId());
    assertThat(committed.getStatus()).isEqualTo("COMMITTED");
    
    // 5. Customer views menu
    List<InventoryItemResponse> menu = getCustomerMenu(location.getId());
    
    // 6. Verify display requirements
    assertThat(menu).hasSize(4); // Excludes unavailable item
    assertThat(menu.get(0).getImageUrl()).isNotNull();
    assertThat(menu.get(0).getCustomerPrice()).isGreaterThan(menu.get(0).getInStorePrice());
    assertThat(menu).extracting("isAvailable").containsOnly(true);
}
```

## Related Test Files
- `test/InventoryUploadDisplayTest.java` (to be created)
- `test/InventoryImportValidationTest.java` (existing - validates CSV parsing)
- `domain-api/src/test/java/com/agentcommerce/domain/inventory/InventoryImportParserTest.java` (existing unit tests)

## Dependencies
- Object storage for images (SPEC 583)
- Configurable platform fee (X%)
- Event bus for inventory updates (SPEC 585, 416)
