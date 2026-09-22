package com.agentcommerce.domain.inventory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.agentcommerce.domain.vendor.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InventoryService {
    private final InventoryRepository repository; private final VendorRepository vendors; private final InventoryImportParser parser;
    public InventoryService(InventoryRepository repository,VendorRepository vendors,InventoryImportParser parser){this.repository=repository;this.vendors=vendors;this.parser=parser;}
    public List<InventoryItemResponse> vendorInventory(UUID userId,UUID locationId){requireManager(userId,locationId);return repository.list(locationId,false);}
    public List<InventoryItemResponse> availableMenu(UUID locationId){return repository.list(locationId,true);}
    public InventoryItemResponse upsert(UUID userId,UUID locationId,UpsertInventoryItemRequest request){requireManager(userId,locationId);return repository.upsert(locationId,request,"MANUAL");}
    public InventoryImportResponse stage(UUID userId,UUID locationId,MultipartFile file){requireManager(userId,locationId);if(file.isEmpty())throw new IllegalArgumentException("CSV file is required");if(file.getSize()>5_000_000)throw new IllegalArgumentException("CSV file exceeds 5 MB");try{byte[] bytes=file.getBytes();String text=new String(bytes,StandardCharsets.UTF_8);return repository.stage(userId,locationId,file.getOriginalFilename()==null?"inventory.csv":file.getOriginalFilename(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),parser.parse(text));}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Unable to process CSV",e);}}
    public InventoryImportResponse getImport(UUID userId,UUID locationId,UUID importId){requireManager(userId,locationId);InventoryImportResponse value=repository.getImport(importId).orElseThrow(()->new IllegalArgumentException("Inventory import not found"));if(!locationId.equals(value.vendorLocationId()))throw new InventoryAccessDeniedException("Import does not belong to this location");return value;}
    public InventoryImportResponse commit(UUID userId,UUID locationId,UUID importId){getImport(userId,locationId,importId);return repository.commit(importId);}
    private void requireManager(UUID userId,UUID locationId){if(!vendors.canManageLocation(userId,locationId)&&!vendors.hasRole(userId,"OPERATOR"))throw new InventoryAccessDeniedException("An active location manager membership or operator role is required");}
}