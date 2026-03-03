package com.monsoon.seedflowplus.domain.map.dto.response;

import lombok.Getter;
import lombok.Setter;

public class NcpmsApiResponse {

    @Getter @Setter
    public static class NcpmsListDto {
        private String insectKey;
        private String examinYear;
        private String kncrNm; // 작목명
    }

    @Getter @Setter
    public static class NcpmsSidoDto {
        private String insectKey;
        private String sidoCode;
        private String sidoNm;
    }

    @Getter @Setter
    public static class NcpmsSigunguDto {
        private String insectKey;
        private String sidoCode;
        private String sigunguNm;
        private String dbyhsNm; // 병해충명
        private Integer inqireValue; // 조회값 (심각도 산출용)
    }
}