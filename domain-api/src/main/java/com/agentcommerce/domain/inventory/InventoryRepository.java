package com.agentcommerce.domain.inventory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class InventoryRepository {
    private final JdbcClient jdbc;
    InventoryRepository(JdbcClient jdbc){this.jdbc=jdbc;}

    List<InventoryItemResponse> list(UUID locationId, boolean availableOnly){return jdbc.sql("""
        SELECT c.id,c.vendor_location_id,c.sku,c.name,c.description,c.image_url,c.category,c.price_minor,c.currency,
          i.tracking_mode,i.quantity_on_hand,i.quantity_reserved,i.is_available,i.reorder_threshold,i.source,i.version,i.updated_at
        FROM catalog_items c JOIN inventory_records i ON i.catalog_item_id=c.id
        WHERE c.vendor_location_id=:locationId AND (:availableOnly=FALSE OR (c.status='ACTIVE' AND i.is_available=TRUE AND (i.tracking_mode='AVAILABILITY_ONLY' OR i.quantity_on_hand-i.quantity_reserved>0)))
        ORDER BY c.name,c.sku""").param("locationId",locationId).param("availableOnly",availableOnly).query(this::mapItem).list();}

    Optional<InventoryItemResponse> findBySku(UUID locationId,String sku){return jdbc.sql("""
        SELECT c.id,c.vendor_location_id,c.sku,c.name,c.description,c.image_url,c.category,c.price_minor,c.currency,
          i.tracking_mode,i.quantity_on_hand,i.quantity_reserved,i.is_available,i.reorder_threshold,i.source,i.version,i.updated_at
        FROM catalog_items c JOIN inventory_records i ON i.catalog_item_id=c.id WHERE c.vendor_location_id=:locationId AND lower(c.sku)=lower(:sku)""").param("locationId",locationId).param("sku",sku).query(this::mapItem).optional();}

    @Transactional InventoryItemResponse upsert(UUID locationId,UpsertInventoryItemRequest request,String source){Optional<InventoryItemResponse> existing=findBySku(locationId,request.sku());if(existing.isPresent()&&request.version()!=null&&request.version()!=existing.get().version())throw new InventoryConflictException("Inventory changed; reload and retry");UUID itemId=existing.map(InventoryItemResponse::id).orElseGet(UUID::randomUUID);jdbc.sql("""
        INSERT INTO catalog_items(id,vendor_location_id,sku,name,description,image_url,category,price_minor,currency,status)
        VALUES(:id,:locationId,:sku,:name,:description,:imageUrl,:category,:price,:currency,'ACTIVE')
        ON CONFLICT(vendor_location_id,sku) DO UPDATE SET name=excluded.name,description=excluded.description,image_url=excluded.image_url,category=excluded.category,price_minor=excluded.price_minor,currency=excluded.currency,status='ACTIVE',updated_at=now(),version=catalog_items.version+1""").param("id",itemId).param("locationId",locationId).param("sku",request.sku().trim()).param("name",request.name().trim()).param("description",request.description()).param("imageUrl",request.imageUrl()).param("category",request.category()).param("price",request.priceMinor()).param("currency",request.currency()==null?"USD":request.currency()).update();itemId=findByCatalogSku(locationId,request.sku());jdbc.sql("""
        INSERT INTO inventory_records(id,catalog_item_id,vendor_location_id,tracking_mode,quantity_on_hand,is_available,reorder_threshold,source)
        VALUES(:id,:itemId,:locationId,:mode,:quantity,:available,:threshold,:source)
        ON CONFLICT(catalog_item_id) DO UPDATE SET tracking_mode=excluded.tracking_mode,quantity_on_hand=excluded.quantity_on_hand,is_available=excluded.is_available,reorder_threshold=excluded.reorder_threshold,source=excluded.source,updated_at=now(),version=inventory_records.version+1""").param("id",UUID.randomUUID()).param("itemId",itemId).param("locationId",locationId).param("mode",request.trackingMode()==null?"QUANTITY":request.trackingMode()).param("quantity",request.quantityOnHand()).param("available",request.available()).param("threshold",request.reorderThreshold()).param("source",source).update();return findBySku(locationId,request.sku()).orElseThrow();}
    private UUID findByCatalogSku(UUID locationId,String sku){return jdbc.sql("SELECT id FROM catalog_items WHERE vendor_location_id=:locationId AND lower(sku)=lower(:sku)").param("locationId",locationId).param("sku",sku).query(UUID.class).single();}

    @Transactional InventoryImportResponse stage(UUID userId,UUID locationId,String fileName,String hash,List<ParsedInventoryRow> rows){UUID id=UUID.randomUUID();int valid=(int)rows.stream().filter(ParsedInventoryRow::valid).count();jdbc.sql("INSERT INTO inventory_imports(id,vendor_location_id,submitted_by_user_id,file_name,content_hash,status,total_rows,valid_rows,invalid_rows) VALUES(:id,:locationId,:userId,:name,:hash,'STAGED',:total,:valid,:invalid)").param("id",id).param("locationId",locationId).param("userId",userId).param("name",fileName).param("hash",hash).param("total",rows.size()).param("valid",valid).param("invalid",rows.size()-valid).update();for(ParsedInventoryRow row:rows)jdbc.sql("""
        INSERT INTO inventory_import_rows(id,inventory_import_id,row_number,sku,name,image_url,category,price_minor,currency,quantity_on_hand,is_available,reorder_threshold,is_valid,included,error_message)
        VALUES(:id,:importId,:number,:sku,:name,:imageUrl,:category,:price,:currency,:quantity,:available,:threshold,:valid,:valid,:error)""").param("id",UUID.randomUUID()).param("importId",id).param("number",row.rowNumber()).param("sku",row.sku()).param("name",row.name()).param("imageUrl",row.imageUrl()).param("category",row.category()).param("price",row.priceMinor()).param("currency",row.currency()).param("quantity",row.quantityOnHand()).param("available",row.available()).param("threshold",row.reorderThreshold()).param("valid",row.valid()).param("error",row.error().isEmpty()?null:row.error()).update();return getImport(id).orElseThrow();}
    Optional<InventoryImportResponse> getImport(UUID id){var header=jdbc.sql("SELECT id,vendor_location_id,file_name,status,total_rows,valid_rows,invalid_rows,version,created_at FROM inventory_imports WHERE id=:id").param("id",id).query((rs,n)->new ImportHeader(rs.getObject("id",UUID.class),rs.getObject("vendor_location_id",UUID.class),rs.getString("file_name"),rs.getString("status"),rs.getInt("total_rows"),rs.getInt("valid_rows"),rs.getInt("invalid_rows"),rs.getLong("version"),rs.getTimestamp("created_at").toInstant())).optional();return header.map(h->new InventoryImportResponse(h.id(),h.locationId(),h.fileName(),h.status(),h.total(),h.valid(),h.invalid(),h.version(),h.createdAt(),rows(id)));}
    private List<InventoryImportRowResponse> rows(UUID id){return jdbc.sql("SELECT * FROM inventory_import_rows WHERE inventory_import_id=:id ORDER BY row_number").param("id",id).query((rs,n)->new InventoryImportRowResponse(rs.getObject("id",UUID.class),rs.getInt("row_number"),rs.getString("sku"),rs.getString("name"),rs.getString("image_url"),rs.getString("category"),(Long)rs.getObject("price_minor"),rs.getString("currency"),(Integer)rs.getObject("quantity_on_hand"),(Boolean)rs.getObject("is_available"),(Integer)rs.getObject("reorder_threshold"),rs.getBoolean("is_valid"),rs.getBoolean("included"),rs.getString("error_message"))).list();}
    @Transactional InventoryImportResponse commit(UUID importId){InventoryImportResponse value=getImport(importId).orElseThrow(()->new IllegalArgumentException("Inventory import not found"));if(!"STAGED".equals(value.status()))throw new InventoryConflictException("Only staged imports can be committed");for(var row:value.rows())if(row.valid()&&row.included()){var request=new UpsertInventoryItemRequest(row.sku(),row.name(),null,row.imageUrl(),row.category(),row.priceMinor(),row.currency(),"QUANTITY",row.quantityOnHand(),row.available(),row.reorderThreshold(),null);InventoryItemResponse item=upsert(value.vendorLocationId(),request,"CSV");jdbc.sql("UPDATE inventory_import_rows SET committed_item_id=:itemId WHERE id=:id").param("itemId",item.id()).param("id",row.id()).update();}jdbc.sql("UPDATE inventory_imports SET status='COMMITTED',committed_at=now(),version=version+1 WHERE id=:id AND status='STAGED'").param("id",importId).update();return getImport(importId).orElseThrow();}
    private InventoryItemResponse mapItem(ResultSet rs,int row)throws SQLException{return new InventoryItemResponse(rs.getObject("id",UUID.class),rs.getObject("vendor_location_id",UUID.class),rs.getString("sku"),rs.getString("name"),rs.getString("description"),rs.getString("image_url"),rs.getString("category"),rs.getLong("price_minor"),rs.getString("currency"),rs.getString("tracking_mode"),rs.getInt("quantity_on_hand"),rs.getInt("quantity_reserved"),rs.getBoolean("is_available"),rs.getInt("reorder_threshold"),rs.getString("source"),rs.getLong("version"),rs.getTimestamp("updated_at").toInstant());}
    private record ImportHeader(UUID id,UUID locationId,String fileName,String status,int total,int valid,int invalid,long version,java.time.Instant createdAt){}
}
