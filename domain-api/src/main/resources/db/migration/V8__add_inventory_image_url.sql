ALTER TABLE catalog_items
    ADD COLUMN image_url VARCHAR(512);

ALTER TABLE inventory_import_rows
    ADD COLUMN image_url VARCHAR(512);
