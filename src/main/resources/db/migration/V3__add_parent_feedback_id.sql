ALTER TABLE tbl_product_feedback
    ADD COLUMN parent_feedback_id BIGINT NULL,
    ADD CONSTRAINT fk_product_feedback_parent
        FOREIGN KEY (parent_feedback_id) REFERENCES tbl_product_feedback (product_feedback_id)
            ON DELETE CASCADE;
    ADD INDEX idx_product_feedback_parent (parent_feedback_id);