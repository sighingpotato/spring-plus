# 🚀 Spring Plus

기존의 일정 관리 시스템(Legacy)을 최신 스프링 부트(Spring Boot 3.x) 환경에 맞추어 리팩토링하고, 성능 최적화 및 안정성을 높이는 고도화 프로젝트입니다. 초기 기본 기능 구현부터 대용량 데이터 처리, 보안 프레임워크 도입까지 단계적으로 시스템을 개선했습니다.

## 📌 기술 스택
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Database**: MySQL, Spring Data JPA
- **Security**: Spring Security, JWT (JSON Web Token)
- **Query**: QueryDSL 5.0.0
- **Build Tool**: Gradle

---

## 🛠️ 단계별 구현 내용 및 트러블슈팅

### [Level 1] 기본 기능 구현 및 시스템 안정화 (1~7번)

#### 1 ~ 3. 도메인 설계 및 기본 JWT 필터 구현
- `User`, `Todo`, `Manager`, `Comment` 엔티티 간의 연관관계를 설정하고, JPA Auditing(`Timestamped`)을 적용하여 생성/수정 시간을 자동화했습니다.
- 초창기 인증/인가를 위해 서블릿 필터(`Filter`) 기반의 커스텀 `JwtFilter`를 구현하여 토큰 발급 및 검증 로직을 구축했습니다. *(이후 Level 2에서 Spring Security로 고도화)*

#### 4. N+1 문제 해결 (Fetch Join 적용)
- **문제**: 일정을 단건 조회하거나 목록을 조회할 때, 연관된 유저(`User`) 정보를 가져오기 위해 불필요한 추가 쿼리가 다수 발생하는 N+1 문제 확인.
- **해결**: `TodoRepository`의 JPQL에 `LEFT JOIN FETCH`를 적용하여, 일정과 유저 데이터를 쿼리 한 방에 가져오도록 성능을 개선했습니다.

#### 5. AOP를 활용한 관리자 접근 로깅 (`AdminAccessLoggingAspect`)
- 비즈니스 로직과 부가 기능(로그 기록)을 분리하기 위해 Spring AOP를 도입했습니다.
- `/admin/**` 경로로 들어오는 관리자 전용 API 호출 시, 요청한 유저의 ID와 요청 URL, 접근 시간을 자동으로 로깅하도록 구현했습니다.

#### 6 ~ 7. 예외 처리 고도화 및 테스트 코드 작성
- `@RestControllerAdvice`를 활용해 전역 예외 처리(Global Exception Handling)를 구축하고, `InvalidRequestException` 등 커스텀 예외를 적용해 클라이언트에게 명확한 에러 메시지를 전달하도록 개선했습니다.
- 핵심 비즈니스 로직에 대한 단위 테스트 및 통합 테스트 코드를 작성하여 시스템 안정성을 확보했습니다.

---

### [Level 2] 코드 리팩토링 및 환경 마이그레이션 (8~9번)

#### 8. QueryDSL 환경 구축 및 명시적 기능 선언 마이그레이션
기존 JPQL로 작성되어 동적 쿼리 작성과 유지보수가 어려웠던 복잡한 쿼리들을 타입 안정성이 보장되는 QueryDSL로 마이그레이션했습니다.

- **Troubleshooting: `javax` vs `jakarta` 패키지 충돌 에러**
  - **문제**: Spring Boot 3.x 환경에서 QueryDSL 설정에 `:jpa` 꼬리표를 사용하여 `Unable to load class 'javax.persistence.Entity'` 컴파일 에러 발생.
  - **해결**: `build.gradle`의 annotationProcessor 설정을 `:jakarta`로 명시적으로 변경하여 Q클래스 생성 오류를 해결.
  ```groovy
  annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
  ```
  
  - **구조 개선**: `TodoRepositoryCustom` (인터페이스) -> `TodoRepositoryCustomImpl` (QueryDSL 구현체) -> `TodoRepository`(JPA 다중 상속)의 표준 구조를 확립했습니다.

#### 9. Spring Security 도입 (커스텀 필터 마이그레이션)

서블릿 필터로 직접 제어하던 가내수공업 인증 로직을 스프링 표준 보안 프레임워크인 Spring Security로 전환했습니다.

- **구현 내용**:
  - `WebSecurityConfigurerAdapter`가 폐기된 최신 스펙에 맞추어 람다식 기반의 `SecurityFilterChain` 구성(`SecurityConfig`).
  - 기존 `JwtFilter`를 `OncePerRequestFilter`를 상속받는 `JwtAuthenticationFilter`로 완전히 교체.
- **Troubleshooting: 기존 AOP 로직 호환성 유지**
  - **문제**: 시큐리티 도입 후 기존 Level 1에서 만든 AOP 로깅 로직이 유저 정보를 찾지 못해 정상 작동하지 않을 위험성 존재.
  - **해결**: 필터 검증 완료 후 `request.setAttribute("userId", userId)` 코드를 유지하여, 컨트롤러와 AOP 단의 수정을 최소화하고 100% 하위 호환되도록 설계했습니다.

### [Level 3] 성능 최적화 및 트랜잭션 제어 (10~11번)

#### 10. QueryDSL을 활용한 검색 기능 최적화 (Projections)

특정 조건(제목, 날짜, 닉네임)으로 일정을 검색할 때, 불필요한 엔티티 전체를 메모리에 올리지 않고 **화면에 필요한 정보만** 가져오도록 최적화했습니다.

- **요구사항**: 일정 제목, 담당자 수, 총 댓글 개수만 반환 (페이징 포함)
- **구현 내용**:
  - 전용 반환 그릇인 `TodoSearchResponseDto` 생성.
  - QueryDSL의 `Projections.constructor()`를 활용하여 데이터베이스 조회 단계에서부터 DTO로 직접 맵핑.
  - `manager.countDistinct()`, `comment.countDistinct()`와 `leftJoin`, `groupBy`를 결합하여 N+1 문제를 방지하고 쿼리 단 한 방으로 집계 데이터를 추출.
  - `BooleanExpression`을 적용해 조건(검색어)이 존재할 때만 WHERE 절이 발동하는 안전한 동적 쿼리를 구현했습니다.

#### 11. 트랜잭션 전파 속성 심화 (`@Transactional(REQUIRES_NEW)`)

매니저 등록 실패 시 예외가 터져 롤백(Rollback)이 발생하더라도, '누가 누구를 등록 시도했다'는 보안 로그 기록은 데이터베이스에 무조건 보존되도록 트랜잭션을 물리적으로 분리했습니다.

- **구현 내용**:

  - 로그 저장을 담당하는 독립적인 `Log` 엔티티와 `LogService` 구현.
  - 매니저 등록 서비스의 기존 트랜잭션과 단절시키기 위해, 로그 저장 메서드에 **`Propagation.REQUIRES_NEW`** 옵션 적용.

  ```java
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveLog(String message) {
      logRepository.save(new Log(message));
  }
  ```

- **결과**: 고의로 예외(자신을 매니저로 등록 시도 등)를 발생시켜 메인 비즈니스 로직이 롤백되더라도, 로그 테이블(`log`)에는 요청 이력이 안전하게 영속화되는 것을 확인했습니다.