package com.monsoon.seedflowplus.domain.map.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPestForecast is a Querydsl query type for PestForecast
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPestForecast extends EntityPathBase<PestForecast> {

    private static final long serialVersionUID = 1348243201L;

    public static final QPestForecast pestForecast = new QPestForecast("pestForecast");

    public final StringPath areaName = createString("areaName");

    public final StringPath cropCode = createString("cropCode");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath pestCode = createString("pestCode");

    public final StringPath severity = createString("severity");

    public final StringPath sidoCode = createString("sidoCode");

    public final StringPath sigunguCode = createString("sigunguCode");

    public QPestForecast(String variable) {
        super(PestForecast.class, forVariable(variable));
    }

    public QPestForecast(Path<? extends PestForecast> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPestForecast(PathMetadata metadata) {
        super(PestForecast.class, metadata);
    }

}

