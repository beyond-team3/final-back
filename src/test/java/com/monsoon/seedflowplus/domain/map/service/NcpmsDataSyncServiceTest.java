package com.monsoon.seedflowplus.domain.map.service;

import com.monsoon.seedflowplus.domain.map.repository.PestForecastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class NcpmsDataSyncServiceTest {

    private NcpmsDataSyncService ncpmsDataSyncService;
    private PestForecastRepository pestForecastRepository;

    @BeforeEach
    void setUp() {
        pestForecastRepository = Mockito.mock(PestForecastRepository.class);
        ncpmsDataSyncService = new NcpmsDataSyncService(pestForecastRepository);
    }

    @Test
    @DisplayName("작물명 매핑 테스트: 정상 케이스")
    void mapCropNameToCode_ValidName_ReturnsCode() {
        assertThat(ncpmsDataSyncService.mapCropNameToCode("배추")).isEqualTo("cabbage");
        assertThat(ncpmsDataSyncService.mapCropNameToCode(" 고추 ")).isEqualTo("pepper"); // 공백 제거 확인
        assertThat(ncpmsDataSyncService.mapCropNameToCode("마늘")).isEqualTo("garlic");
    }

    @Test
    @DisplayName("작물명 매핑 테스트: 비정상 케이스")
    void mapCropNameToCode_InvalidName_ReturnsUnknown() {
        assertThat(ncpmsDataSyncService.mapCropNameToCode("존재하지않는작물")).isEqualTo("UNKNOWN");
        assertThat(ncpmsDataSyncService.mapCropNameToCode(null)).isEqualTo("UNKNOWN");
        assertThat(ncpmsDataSyncService.mapCropNameToCode("")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("병해충명 매핑 테스트: 정상 케이스")
    void mapPestNameToCode_ValidName_ReturnsCode() {
        assertThat(ncpmsDataSyncService.mapPestNameToCode("노균병")).isEqualTo("P01");
        assertThat(ncpmsDataSyncService.mapPestNameToCode(" 탄저병 ")).isEqualTo("P03"); // 공백 제거 확인
    }

    @Test
    @DisplayName("병해충명 매핑 테스트: 비정상 케이스")
    void mapPestNameToCode_InvalidName_ReturnsUnknown() {
        assertThat(ncpmsDataSyncService.mapPestNameToCode("가짜병")).isEqualTo("UNKNOWN");
        assertThat(ncpmsDataSyncService.mapPestNameToCode(null)).isEqualTo("UNKNOWN");
        assertThat(ncpmsDataSyncService.mapPestNameToCode("   ")).isEqualTo("UNKNOWN");
    }
}
