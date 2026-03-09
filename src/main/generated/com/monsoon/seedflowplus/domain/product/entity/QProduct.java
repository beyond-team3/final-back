package com.monsoon.seedflowplus.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = 1234844332L;

    public static final QProduct product = new QProduct("product");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final EnumPath<ProductCategory> productCategory = createEnum("productCategory", ProductCategory.class);

    public final StringPath productCode = createString("productCode");

    public final StringPath productDescription = createString("productDescription");

    public final StringPath productImageUrl = createString("productImageUrl");

    public final StringPath productName = createString("productName");

    public final EnumPath<ProductStatus> status = createEnum("status", ProductStatus.class);

    public final MapPath<String, java.util.List<String>, SimplePath<java.util.List<String>>> tags = this.<String, java.util.List<String>, SimplePath<java.util.List<String>>>createMap("tags", String.class, java.util.List.class, SimplePath.class);

    public final StringPath unit = createString("unit");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QProduct(String variable) {
        super(Product.class, forVariable(variable));
    }

    public QProduct(Path<? extends Product> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProduct(PathMetadata metadata) {
        super(Product.class, metadata);
    }

}

