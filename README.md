# SEEDFLOW+

> **[Team MonSoon] 종자 회사를 위한 B2B 영업 관리 솔루션**
>
> "종자에서 수확까지, 데이터로 연결되는 스마트한 영업 파이프라인"

---

## 1. 프로젝트 개요

### 🌿 개발 배경
- **복잡한 유통 구조**: 종자는 생산 후 육묘장이나 지역 농협을 거쳐 농가로 전달되는 복합적인 B2B 유통 경로를 가짐
- **정보의 파편화**: 영업 현장의 정보가 개인 단위로 분산되어 있어 조직적 활용이 어렵고, 품종의 재배 특성 및 시기 파악이 매우 까다로움
- **맞춤형 데이터 요구**: 기후 변화와 병해충 이슈로 인해 거래처별(지역별) 맞춤형 내병성 데이터 및 품종 추천에 대한 요구가 증가

### 🎯 서비스 목표
- **영업 프로세스 통합**: 견적 요청부터 결제까지 전 과정을 디지털화하여 투명하고 효율적인 영업 관리 시스템 구축
- **AI 기반 전략 수립**: 영업 노트를 분석하여 거래처별 맞춤 전략을 자동 생성함으로써 영업 생산성 극대화
- **데이터 기반 의사결정**: 지역별 병해충 정보와 연계된 품종 추천 지도를 통해 과학적인 영업 지원

### ✨ 기대 효과
- **운영 효율성**: 파이프라인 통합 관리를 통해 휴먼 에러를 방지하고 업무 처리 시간 단축
- **영업 성공률 제고**: AI 브리핑 및 시각화 도구를 활용한 설득력 있는 고객 제안 가능
- **지속 가능한 데이터 자산**: 파편화된 영업 현장 데이터를 중앙화하여 기업의 핵심 자산으로 활용

---

## 2. 팀원 소개

<table width="100%">
  <tr>
    <td align="center" width="20%">
      <img src="https://via.placeholder.com/100" width="100px;" alt=""/><br />
      <b>김수진</b><br />
      <sub>FE & BE</sub>
    </td>
    <td align="center" width="20%">
      <img src="https://via.placeholder.com/100" width="100px;" alt=""/><br />
      <b>이건우</b><br />
      <sub>FE & BE & Infra</sub>
    </td>
    <td align="center" width="20%">
      <img src="https://via.placeholder.com/100" width="100px;" alt=""/><br />
      <b>이경민</b><br />
      <sub>FE & BE & RAG</sub>
    </td>
    <td align="center" width="20%">
      <img src="https://via.placeholder.com/100" width="100px;" alt=""/><br />
      <b>이하경</b><br />
      <sub>팀장 & FE & BE & Core</sub>
    </td>
    <td align="center" width="20%">
      <img src="https://via.placeholder.com/100" width="100px;" alt=""/><br />
      <b>정하경</b><br />
      <sub>PM & FE & BE & Design</sub>
    </td>
  </tr>
  <tr>
    <td valign="top">대시보드, 빌링 관리(주문/명세/청구), 품종 유사도</td>
    <td valign="top">상품 관리, CI/CD 구축</td>
    <td valign="top">병해풍 매칭지도, AI 영업브리핑</td>
    <td valign="top">딜 파이프라인, 알림/승인, 통계/일정</td>
    <td valign="top">영업 문서(견적 요청/ 견적 /계약), 계정(거래처/사원), QA</td>
  </tr>
</table>

> [!TIP]
> **디자인 통일**: 모든 팀원이 프론트엔드와 백엔드를 전담하여 개발하였으며, 디자인 시스템의 통일성을 위해 공통 UI 가이드를 준수하여 협업했습니다.

---

## 3. 주요 기능 구성

### 📦 영업 파이프라인 (Sales Pipeline)
- **견적-계약-주문**: RFQ(견적 요청)부터 최종 주문서 생성까지의 워크플로우 자동화
- **실시간 승인 프로세스**: 역할별 권한 제어를 통한 문서 승인/반려 및 실시간 알림 시스템
- **통합 문서 관리**: 표준화된 양식의 PDF 생성 및 재작성(Rewrite) 이력 추적

### 🤖 AI 영업 전략 (RAGseed)
- **맞춤 브리핑**: 영업 노트를 기반으로 AI가 거래처 맞춤형 영업 전략 제안
- **전략 자동 분석**: LLM(Gemini)을 활용한 데이터 요약 및 핵심 인사이트 추출

### 🗺️ 품종 추천 및 시각화
- **병해충 품종 매칭 지도**: 지역별 병해충 발생 정보와 매칭된 최적 품종 추천 시각화
- **품종 유사도 그래프**: 상품 간 관계를 Graph 형태로 표현하여 유사 품종 정보 제공
- **상품 비교 분석**: 여러 상품의 스펙을 한 눈에 비교할 수 있는 대조표 지원

---

## 4. 기술 스택

### 💻 백엔드
![Java 21](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot 3.3.5](https://img.shields.io/badge/SpringBoot%203.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-07405E?style=for-the-badge&logo=hibernate&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![LangChain4j](https://img.shields.io/badge/LangChain4j-1C3C3C?style=for-the-badge&logo=chainlink&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Google%20Gemini-4285F4?style=for-the-badge&logo=googlegemini&logoColor=white)

### 🎨 프론트엔드
![Vue.js 3](https://img.shields.io/badge/Vue.js%203-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Pinia](https://img.shields.io/badge/Pinia-FFE14D?style=for-the-badge&logo=pinia&logoColor=black)
![Chart.js](https://img.shields.io/badge/Chart.js-FF6384?style=for-the-badge&logo=chartdotjs&logoColor=white)
![Leaflet](https://img.shields.io/badge/Leaflet-199900?style=for-the-badge&logo=leaflet&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![NPM](https://img.shields.io/badge/NPM-CB3837?style=for-the-badge&logo=npm&logoColor=white)

### 🗄️ 데이터베이스
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### 🛠️ Tools & Infra
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![CodeRabbit](https://img.shields.io/badge/CodeRabbit-000000?style=for-the-badge&logo=coderabbit&logoColor=white)
![Linear](https://img.shields.io/badge/Linear-5E6AD2?style=for-the-badge&logo=linear&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white)
![Discord](https://img.shields.io/badge/discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![ArgoCD](https://img.shields.io/badge/ArgoCD-EF7B4D?style=for-the-badge&logo=argo&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)
![Amazon EC2](https://img.shields.io/badge/Amazon%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![DataGrip](https://img.shields.io/badge/DataGrip-000000?style=for-the-badge&logo=datagrip&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Ubuntu](https://img.shields.io/badge/Ubuntu-E95420?style=for-the-badge&logo=ubuntu&logoColor=white)

---

## 5. 문서 리스트

- [01. 프로젝트 기획서](https://github.com/beyond-team3/final-back/wiki/01.%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EA%B8%B0%ED%9A%8D%EC%84%9C)
- [02. 요구사항 정의서](https://github.com/beyond-team3/final-back/wiki/02.%EC%9A%94%EA%B5%AC%EC%82%AC%ED%95%AD-%EC%A0%95%EC%9D%98%EC%84%9C)
- [03. 시스템 아키텍처](https://github.com/beyond-team3/final-back/wiki/03.%EC%8B%9C%EC%8A%A4%ED%85%9C-%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98)
- [04. WBS](https://github.com/beyond-team3/final-back/wiki/04.WBS)
- [05. ERD](https://github.com/beyond-team3/final-back/wiki/05.ERD)
- [06. 화면설계서](https://github.com/beyond-team3/final-back/wiki/06.-%ED%99%94%EB%A9%B4-%EC%84%A4%EA%B3%84%EC%84%9C)
- [07. UI/UX 단위테스트 결과서](https://github.com/beyond-team3/final-back/wiki/07.UI-UX-%EB%8B%A8%EC%9C%84%ED%85%8C%EC%8A%A4%ED%8A%B8-%EA%B2%B0%EA%B3%BC%EC%84%9C)
- [08. API 명세서](https://github.com/beyond-team3/final-back/wiki/08.-API-%EB%AA%85%EC%84%B8%EC%84%9C)
- [09. 단위 테스트 결과서](https://github.com/beyond-team3/final-back/wiki/09.%EB%8B%A8%EC%9C%84%ED%85%8C%EC%8A%A4%ED%8A%B8-%EA%B2%B0%EA%B3%BC%EC%84%9C)
- [10. CI/CD 계획서](https://github.com/beyond-team3/final-back/wiki/10.-CI-CD-%EA%B3%84%ED%9A%8D%EC%84%9C)
- [11. 배포 후 통합 테스트 결과서](https://github.com/beyond-team3/final-back/wiki/11.-%EB%B0%B0%ED%8F%AC-%ED%9B%84-%ED%86%B5%ED%95%A9-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EA%B2%B0%EA%B3%BC%EC%84%9C)
- [12. 시연영상](https://github.com/beyond-team3/final-back/wiki/12.-%EC%8B%9C%EC%97%B0-%EC%98%81%EC%83%81)
- [13. PT 자료](https://github.com/beyond-team3/final-back/wiki/13.-PT-%EC%9E%90%EB%A3%8C)

---

## 6. 회고

| 이름 | 회고                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| :--- |:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **김수진** | 이번 프로젝트에서는 페르소나별 대시보드, 계약 기반의 주문·명세·청구·결제 흐름, 그리고 상품 유사도 분석 파트를 담당했습니다. 가장 어려웠던 부분은 연관관계가 많은 작업을 분업하는 것이었습니다. 선행 작업이 완료되기 전에 내 작업을 끝내도 실제 테스트가 불가능했고, 임의로 SQL 더미 데이터를 만들려 해도 ERD 구조가 복잡해 데이터 정합성을 맞추는 것 자체가 쉽지 않았습니다. 결국 테이블 간 관계를 다시 정리하고 최소 단위 데이터부터 단계적으로 생성하는 방식으로 접근해 해결했습니다. 상품 유사도 분석은 어떤 방식을 선택할지, 결과를 어떻게 평가할지가 가장 큰 고민이었습니다. 농업 상품의 특성을 고려해 태그 기반 Jaccard 유사도와 재배적기 월 겹침 분석을 결합한 방식을 설계했고, 연도 경계를 걸치는 재배 시기나 카테고리별 가중치 처리 등 도메인에 맞는 로직을 구현할 수 있었습니다. 프론트와 백엔드를 연결하는 과정도 예상보다 까다로웠습니다. 가정한 데이터 구조와 실제 API 응답이 맞지 않는 경우가 반복됐고, 이를 겪으면서 인터페이스 정의와 사전 커뮤니케이션이 얼마나 중요한지 체감했습니다. 긴 기간 동안 기획부터 개발까지 직접 진행하면서 실질적인 개발 경험을 쌓을 수 있었습니다. 특히 초기 설계 단계에서 ERD와 API 명세를 먼저 명확히 정의해두는 것이 이후 개발 속도와 품질 모두에 영향을 준다는 것을 이번 프로젝트를 통해 배웠습니다.                                                                                                                                                                                                                                                                                                                                                                                            |
| **이건우** | 이번 프로젝트에서 저는 상품 관련 프론트엔드, 백엔드, 그리고 인프라 전반을 담당하며 서비스의 핵심 인프라를 구축하고 애플리케이션을 완성하는 역할을 수행했습니다. 초기에는 유동 IP의 불편함을 개선하고자 Tailscale과 Jenkins를 활용한 CI 파이프라인을 구축하여 개발 환경의 자동화 기반을 마련했습니다. 이 과정에서 전통적인 VPN 대비 구축 비용을 80% 이상 절감하며, 로컬 DB와 AWS 클라우드 간의 안전한 하이브리드 네트워크를 성공적으로 구축했습니다. 이후 Spring Boot 기반의 백엔드 서버를 개발하며 데이터베이스 연동, API 설계, 그리고 비즈니스 로직 구현을 주도했습니다. 특히 영업 관리 시스템의 핵심인 데이터 정합성과 트랜잭션 관리에 집중했습니다. 프로젝트 막바지에는 크레딧 한도 이슈로 데이터베이스를 RDS 환경에서 로컬 서버 환경으로 이전하는 돌발 상황이 발생했으나, RDS 구축 시 설정해둔 자동 백업 시스템 덕분에 데이터 유실 없이 성공적으로 복구할 수 있었습니다. 프론트엔드에서는 React를 활용해 사용자 중심의 상품 페이지 UI를 구현했으며, 영업사원 전용 상품 피드백 커뮤니티 기능을 추가하여 현장의 반응을 실시간으로 공유할 수 있는 시스템을 구축했습니다. 프로젝트 후반에는 팀원들과 협업하여 전체 시스템을 통합하고 안정성을 확보하는 데 주력했습니다. 이 과정에서 인프라 자원 관리의 중요성을 체감했으며, 특히 EKS 환경에서 빌드 에이전트 파드가 ephemeral-storage 부족으로 강제 종료 되는 문제를 해결하며 컨테이너 리소스 쿼터 설정과 빌드 로그 로테이션의 중요성을 학습했습니다. 또한 알림 서비스 트랜잭션 최적화를 통해 공유 DB의 Lock 경합을 해소한 경험은 운영 관점에서 큰 배움이 되었습니다. 마지막으로 ArgoCD를 활용한 Blue-Green 무중단 배포 전략을 채택했습니다, Preview 단계에서 철저한 검증 후 수동 Promote를 통해 배포하거나 에러 발생 시 Reject하는 파이프라인을 설계하여 배포 안정성을 확보했습니다. 이번 프로젝트를 통해 기획부터 개발, 인프라 구축까지 전 과정을 경험하며 풀스택 개발자로서의 역량을 한층 강화할 수 있었습니다. |
| **이경민** | 기획부터 개발까지 쉼 없이 달려온 2개월이었습니다.기획 단계에서는 ‘항상 회의하고 있는 조’로 불릴 만큼 많은 회의를 진행하며 다양한 의견을 나눴습니다. 그 과정에서 의견 충돌로 잠시 지치기도 했지만 단순히 부딪히는 데서 끝나는 것이 아니라 칠판에 의견을 정리하고 더 나은 방향으로 합의해 나갔던 경험이 인상 깊었습니다.개발 단계에서는 병해충-품종 매칭 지도, 영업 노트, AI 기반 영업 브리핑 및 전략 분석 기능을 구현하며 기술에 대한 고민을 이어나갔습니다. 특히 AI 기능을 구현하는 과정에서 RAG, LangChain 등 관련 기술에 대한 이해 부족으로 초기에는 어려움을 겪었지만 멘토링을 통해 개념과 구조를 먼저 학습한 뒤 적용하면서 기능을 완성할 수 있었습니다. 이 과정에서 단순 구현을 넘어 새로운 기술을 빠르게 이해하고 실제 서비스에 적용하는 경험을 쌓을 수 있었습니다.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| **이하경** | 이번 프로젝트에서 팀장 역할을 맡아 초기 협업 환경 구축부터 전체 개발 흐름을 정리하는 일을 담당했습니다. Linear와 GitHub 기반의 협업 구조를 설정하며 브랜치 전략과 작업 프로세스를 정립하는 과정에서 시행착오를 겪었지만, 이를 통해 팀원 간 작업 충돌을 줄일 수 있었습니다. 영업 관리 시스템의 핵심 구조인 SalesDeal 기반 영업 파이프라인을 설계하고 백엔드 구현을 주도하면서 도메인 구조를 깊이 이해하게 되었습니다. 알림, 승인, 일정, 통계와 같은 필수 업무 지원 기능을 프론트엔드와 백엔드 양쪽에서 직접 구현하며 서비스 전반의 흐름을 연결하는 경험을 했습니다. 특히 승인 기능 구현 과정에서는 의존성 관리와 구조 설계의 중요성을 체감했고, 무리한 확장보다 일관된 설계를 유지하는 방향으로 개선했습니다. 통계 기능 구현에서는 조회 기간 제한, 데이터 검증 기준 등 정책 정의가 부족해 반복 수정이 발생했으며, 이후 명확한 도메인 규칙을 먼저 설계하는 방식으로 전환했습니다. 공유 DB 환경에서 ddl-auto 설정으로 인해 트랜잭션이 충돌하는 문제를 경험하며, 운영 환경에서는 명시적인 스키마 관리가 필요하다는 점을 배웠습니다. 프로젝트 전반을 지속적으로 점검하고 팀원들의 작업을 리뷰하며, 기술적인 완성도뿐 아니라 협업의 일관성을 유지하는 데 집중했습니다. 그 과정에서 단순 구현을 넘어 시스템 구조와 운영까지 고려하는 시야를 갖게 되었습니다. 이번 경험을 통해 복잡한 서비스에서도 전체 흐름을 설계하고 조율할 수 있는 역량을 크게 성장시킬 수 있었습니다.                                                                                                                                                                                                                                                                                                                          |                                                                                                                                                    |
| **정하경** | 이번 프로젝트에서 PM(Project Manager)으로서 프로젝트 초기 WBS(Work Breakdown Structure) 수립과 전반적인 일정 관리를 총괄했습니다. 프로젝트 초기, 주제 선정 과정에서 팀원 간 다양한 의견 충돌이 있었으나, 각 안건의 실현 가능성과 시장성을 분석한 근거 중심의 기획서를 우선 작성하여 팀의 합의를 이끌어냈습니다. 또한, 효율적인 일정 관리를 위해 매일 아침 10분간의 데일리 브리핑(09:10~09:20) 시스템을 도입하여, 업무의 우선순위를 명확히 공유함으로써 협업의 시너지를 극대화했습니다. 기술적으로는 영업(견적요청서/견적서/계약서)과 계정(사원/거래처/사용자) 도메인의 풀스택 구현과 전체 시스템 QA를 담당했습니다. 특히 개발 과정에서 '단순 문서 생성(기반 작성 / 신규 작성)'을 넘어 반려 시 기존 데이터를 기반으로 '문서 재작성(Rewrite)'이라는 안건을 제안하며 정책적 변화를 주도했습니다. 이 과정에서 핵심 도메인인 SalesDeal 구조에 많은 고민이 필요했지만, 결과적으로 거래처의 히스토리 추적성을 확보하고 영업 데이터의 정합성을 높임으로써, 핵심 도메인인 SalesDeal 구조의 비즈니스 완성도를 한층 강화했습니다. 또한, 거래처 계정 관리 및 정보 등록 정책 수립 시 멘토링 피드백을 적극적으로 수용하여 관리자 중심의 폐쇄형 관리 체계를 구축함으로써, 보안과 데이터 무결성이 핵심인 B2B 솔루션의 특성을 고려한 결정이었으며, 본 프로그램의 '사내 전용 B2B 솔루션'이라는 정체성을 확고히 할 수 있었습니다. 개발 완료 후에는 팀 내 QA를 전담하며, 단순한 기능 구현보다 프로그램 전체의 안정성과 점검이 얼마나 중요한지 다시 한번 깨달았습니다. 이번 프로젝트를 통해 서비스 구축은 개발뿐만 아니라 기획과 테스트가 유기적으로 긴밀하게 연결되어야 한다는 중요한 실무적 감각을 익힐 수 있었습니다                                                                                                                                                         |


### 🛠️ 트러블 슈팅 (Troubleshooting)
- **DB 데드락 및 부팅 지연 해결**: 공유 DB 환경에서 `ddl-auto: update` 설정으로 인한 DDL 실행이 타 세션의 장기 트랜잭션과 충돌하여 애플리케이션 부팅이 대기 상태에 머무르는 현상 발생. 이를 해결하기 위해 설정을 `validate`로 변경하고, 스키마 변경 시 팀원 합의 하에 수동 SQL 적용 또는 Flyway 마이그레이션을 도입하여 안정성을 확보함.
- **Jenkins 빌드 중단 및 Pod 용량 부족 해결**: 서버 환경 변경 과정에서 누적된 설정 데이터와 임시 파일로 인해 Kubernetes Pod의 가용 용량이 부족해져 Jenkins 빌드가 `abort` 상태에서 멈추는 문제 발생. 이를 해결하기 위해 포스트 빌드(Post-build) 단계에 임시 파일들을 자동으로 삭제하는 스크립트를 추가하여 빌드 안정성을 자동화함.

### 🚀 향후 발전 방향
- **데이터 분석 고도화**: 축적된 영업 히스토리를 바탕으로 정교한 매출 예측 및 통계 시뮬레이션 지원
- **오프라인 연동**: 네트워크 불안정 상황에서도 영업 노트를 작성할 수 있는 오프라인 수기 모드 지원
- **모바일 비즈니스**: 현장 영업 특성에 최적화된 모바일 전용 UI/UX 및 네이티브 기능 연동
