# final-back

## 애플리케이션 설정 가이드 (Spring Profiles)

현재 프로젝트는 설정을 프로파일별로 분리해서 사용합니다.

- 공통 설정: `src/main/resources/application.yml`
- 로컬 설정: `src/main/resources/application-local.yml`
- 테스트 설정: `src/main/resources/application-test.yml`
- 운영 설정: `src/main/resources/application-prod.yml`
- 상세 참조 문서: `src/main/resources/APPLICATION_CONFIG_GUIDE.md` -> AI 사용할 때 사용

### 1) 기본 활성 프로파일

- 기본값: `local`
- 위치: `application.yml`의 `spring.profiles.active`

### 2) 공통 설정 (`application.yml`)

공통 파일에는 모든 환경에서 공유할 값만 둡니다.

- 앱 이름: `spring.application.name: armageddon`
- JPA 정책(이전 프로젝트 기준):
  - `spring.jpa.open-in-view: false`
  - `spring.jpa.hibernate.ddl-auto: update`
  - `spring.jpa.show-sql: false`
  - `spring.jpa.properties.hibernate.format_sql: true`
- 서버 포트: `server.port: 8080`
- 로깅 레벨: `logging.level.root: info`
- Swagger/Springdoc:
  - `/v3/api-docs`
  - `/swagger-ui.html`

### 3) 로컬 설정 (`application-local.yml`)

민감정보 및 로컬 실행 의존 설정을 둡니다.

- DB
  - URL: `jdbc:mariadb://100.81.161.92:3031/nexeed_db`
  - Driver: `org.mariadb.jdbc.Driver`
  - 계정: `${DB_USERNAME}`, `${DB_PASSWORD}`
- Redis
  - `${REDIS_HOST}`, `${REDIS_PORT}`, `${REDIS_PASSWORD}`
- Mail
  - Gmail SMTP 기반
  - `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`
- JWT
  - `${JWT_SECRET}`
  - 만료시간: `${JWT_ACCESS_TOKEN_EXPIRATION_MS}`, `${JWT_REFRESH_TOKEN_EXPIRATION_MS}`

### 4) 테스트 설정 (`application-test.yml`)

테스트에서는 H2 메모리 DB를 사용합니다.

- URL: `jdbc:h2:mem:testdb;MODE=MySQL`
- Driver: `org.h2.Driver`
- `spring.jpa.hibernate.ddl-auto: create-drop`

즉, 테스트 시작 시 스키마 생성, 종료 시 삭제됩니다.

### 5) 운영 설정 (`application-prod.yml`)

운영 환경은 환경변수 주입을 전제로 합니다.

- DB: `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`
- Redis: `${REDIS_HOST}`, `${REDIS_PORT}`, `${REDIS_PASSWORD}`
- Mail: `${MAIL_HOST}`, `${MAIL_PORT}`, `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`
- JWT: `${JWT_SECRET}`, `${JWT_ACCESS_TOKEN_EXPIRATION_MS}`, `${JWT_REFRESH_TOKEN_EXPIRATION_MS}`

운영 모니터링(예: Grafana/Prometheus) 연계를 위해 아래가 열려 있습니다.

- `management.endpoints.web.exposure.include: health,info,prometheus`
- `management.endpoint.health.probes.enabled: true`

### 6) 실행 시 기대 동작

- 프로파일 미지정 실행: `local` 적용
- 테스트 실행: `test` 프로파일에서 H2 + `create-drop` 적용
- 운영 배포: `prod` 프로파일 + CI/CD(Jenkins/ArgoCD) 환경변수 주입

### 7) 주의사항

`.gitignore`에 `application-*.yml` 패턴이 있어 프로파일 파일이 Git 추적에서 제외될 수 있습니다.

## Commit Convention
[리니어ID] type(scope): subject

### type
- feat     : 새로운 기능 추가
- fix      : 버그 수정
- refactor : 기능 변화 없는 코드 구조 개선
- docs     : 문서 수정
- test     : 테스트 코드 추가/수정
- chore    : 빌드, 설정, 기타 작업

### scope
- 변경된 기능 및 분야
- 예: login, transaction, cashflow, readme

### subject
- 변경 내용을 간결하게 작성
- 예: controller 초안 작성

### example
> [BAC-13] feat(user): AuthController 메소드 추가
