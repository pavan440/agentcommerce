package com.agentcommerce.domain.inventory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.UUID;
import com.agentcommerce.domain.vendor.VendorRepository;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
 private final InventoryRepository inventory=mock(InventoryRepository.class);private final VendorRepository vendors=mock(VendorRepository.class);private final InventoryService service=new InventoryService(inventory,vendors,new InventoryImportParser());
 @Test void blocksInventoryReadWithoutVendorMembershipOrOperatorRole(){UUID user=UUID.randomUUID(),location=UUID.randomUUID();when(vendors.canManageLocation(user,location)).thenReturn(false);when(vendors.hasRole(user,"OPERATOR")).thenReturn(false);assertThatThrownBy(()->service.vendorInventory(user,location)).isInstanceOf(InventoryAccessDeniedException.class);verifyNoInteractions(inventory);}
 @Test void delegatesAuthorizedVendorInventoryRead(){UUID user=UUID.randomUUID(),location=UUID.randomUUID();when(vendors.canManageLocation(user,location)).thenReturn(true);service.vendorInventory(user,location);verify(inventory).list(location,false);verify(vendors,never()).hasRole(user,"OPERATOR");}
 @Test void delegatesOperatorInventoryRead(){UUID user=UUID.randomUUID(),location=UUID.randomUUID();when(vendors.canManageLocation(user,location)).thenReturn(false);when(vendors.hasRole(user,"OPERATOR")).thenReturn(true);service.vendorInventory(user,location);verify(inventory).list(location,false);}
}
