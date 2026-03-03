CREATE INDEX idx_products_name_trgm
    ON products
    USING gin (name gin_trgm_ops);