# Git Commit Prompt

아래 지시를 그대로 따라 Git 커밋을 만들어라.

## 목표
- 모든 커밋 메시지는 `README.md`의 Commit Convention을 따른다.
- 변경사항을 1개 커밋으로 몰지 말고, **의미 있는 단위**로 나눠 커밋한다.

## 커밋 메시지 규칙
- 형식: `[리니어ID] type(scope): subject`
- 예시: `BAC-13 feat(user): AuthController 메소드 추가`

### type
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 기능 변화 없는 구조 개선
- `docs`: 문서 수정
- `test`: 테스트 추가/수정
- `chore`: 빌드/설정/기타 작업

### scope
- 변경된 기능/분야를 짧게 작성
- 예: `login`, `transaction`, `cashflow`, `readme`

### subject
- 무엇이 바뀌었는지 간결하게 작성
- 한국어로 작성 가능, 불필요한 접두어 금지

## 커밋 분할 기준 (필수)
아래 기준으로 커밋을 나눠라.

1. 서로 다른 목적은 분리  
   - 기능 추가(`feat`)와 버그 수정(`fix`)를 한 커밋에 섞지 않는다.
2. 레이어가 달라도 목적이 같으면 함께 가능  
   - 같은 기능을 위한 `controller/service/repository` 변경은 1커밋 가능.
3. 리팩토링은 기능 변경과 분리  
   - 동작에 영향 없는 rename/move/cleanup은 `refactor`로 별도 커밋.
4. 문서/테스트/설정은 코드 변경과 분리  
   - `docs`, `test`, `chore`는 가능한 독립 커밋.
5. 리뷰 가능한 크기 유지  
   - 커밋 하나가 너무 크면 하위 기능 단위로 분할.

## 작업 절차
1. 변경 파일을 목적별로 그룹핑한다.
2. 각 그룹에 대해 `type/scope/subject`를 정한다.
3. 커밋 순서를 의존성 기준으로 정한다.
4. 그룹별로 `git add <파일들>` 후 커밋한다.
5. 마지막에 커밋 목록을 보여준다.

## 출력 형식
아래 형식으로 답변하라.

1. `커밋 계획`
- 커밋 1: `[리니어ID] type(scope): subject`
- 포함 파일: `...`
- 분리 이유: `...`

2. `실행 명령`
```bash
git add ...
git commit -m "[리니어ID] type(scope): subject"
```

3. `최종 커밋 요약`
- `git log --oneline -n <커밋수>` 기준으로 결과 정리

## 입력 변수
- `LINEAR_ID`: 예) `BAC-13`
- `변경 파일 목록`
- `변경 요약`

위 입력이 부족하면, 합리적으로 가정하되 그 가정을 먼저 명시한다.
