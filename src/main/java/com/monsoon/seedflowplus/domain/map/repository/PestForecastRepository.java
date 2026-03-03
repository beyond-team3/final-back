package com.monsoon.seedflowplus.domain.map.repository;

import com.monsoon.seedflowplus.domain.map.entity.PestForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PestForecastRepository extends JpaRepository<PestForecast, Long> {
    List<PestForecast> findAllByPestCode(String pestCode);
}