# BUDDYS-SERVER

BUDDYS 서비스의 백엔드 레포지토리입니다.

<br />

## 📌 Service Introduction

<!-- TODO: 서비스 소개 이미지 추가 -->

<br />
<br />

## 👥 Contributors

<div align="center">
  <table>
    <thead>
      <tr>
        <th>이해령</th>
        <th>이지현</th>
        <th>김가윤</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td align="center"><!-- TODO: 프로필 이미지 추가 --></td>
        <td align="center"><!-- TODO: 프로필 이미지 추가 --></td>
        <td align="center"><!-- TODO: 프로필 이미지 추가 --></td>
      </tr>
      <tr align="center">
        <td>Backend</td>
        <td>Backend</td>
        <td>Backend</td>
      </tr>
      <tr align="center">
        <td>
          <a href="https://github.com/haerxeong" target="_blank">@haerxeong</a>
        </td>
        <td>
          <a href="https://github.com/jyvnee" target="_blank">@jyvnee</a>
        </td>
        <td>
          <a href="https://github.com/beneruufin" target="_blank">@beneruufin</a>
        </td>
      </tr>
    </tbody>
  </table>
</div>

<br />
<br />

## 🛠️ Tech Stack

| 분류 | 기술 |
| --- | --- |
| 언어 / 프레임워크 | Java 17, Spring Boot 4.1.0, Spring Web MVC |
| 빌드 | Gradle |
| 인증 / 보안 | Spring Security, OAuth 2.0, JWT, JJWT |
| 소셜 로그인 | Kakao Login |
| 데이터 접근 | Spring Data JPA, Hibernate, QueryDSL, Jakarta Validation, Flyway |
| 데이터베이스 / 저장소 | MySQL, AWS RDS for MySQL, AWS S3 |
| 실시간 통신 | WebSocket, STOMP |
| 인프라 / 배포 | AWS EC2, Docker Compose, Nginx, GitHub Actions |
| API 문서화 | springdoc-openapi, Swagger UI |
| 테스트 | JUnit 5, Spring Boot Test, Mockito, Testcontainers |
| 운영 / 모니터링 | Spring Boot Actuator, Prometheus, Grafana |
| 코드 품질 / 협업 | CodeRabbit |
| Planned | Redis |

<br />
<br />

## 🗂️ Convention

<details>
<summary> 📦 패키지 전략 </summary>
<br/>

#### 도메인형 구조 (Domain-based Package)

프로젝트는 **도메인 중심 패키지 구조**를 적용했습니다. 기능(도메인)별로 관련된 Controller, Service, Repository, Entity 등을 하나의 패키지에 모아 응집도를 높이고, 기능 추가 및 유지보수가 용이하도록 구성했습니다.

```
org.sopt.buddys
├── domain
│   ├── auth
│   ├── user
│   ├── post
│   ├── comment
│   ├── chat
│   ├── tag
│   ├── image
│   ├── location
│   └── recommendation
└── global
    ├── config
    ├── security
    ├── exception
    ├── response
    ├── logging
    ├── aws
    ├── swagger
    └── common
```

각 도메인 패키지 하위에는 `controller`, `service`, `repository`, `entity`, `dto`, `code` 등으로 세분화합니다.

</details>

<details>
<summary> 🎨 코드 스타일 </summary>
<br/>

- 문자열을 다룰 때는 큰따옴표(`"`)를 사용합니다.
- 문장이 종료될 때는 세미콜론(`;`)을 붙입니다.
- 메서드명, 변수명은 카멜케이스(camelCase)로 작성합니다.
- 가독성을 위해 한 줄에 하나의 문장만 작성합니다.
- 주석은 설명하려는 구문에 맞춰 들여쓰기 합니다.

```java
// Good
public void someMethod() {
    ...

    // statement에 관한 주석
    statement;
}
```

- 연산자 사이에는 공백을 추가하여 가독성을 높입니다.

```java
a+b+c+d // bad
a + b + c + d // good
```

- 콤마 다음에 값이 올 경우 공백을 추가하여 가독성을 높입니다.

```java
List.of(1,2,3,4); // bad
List.of(1, 2, 3, 4); // good
```

</details>

<details>
<summary> 📌 패키지명 (Package Naming) </summary>
<br/>

- **스네이크 케이스 (snake_case)** 사용
- 모두 **소문자**로 작성
- 단어 구분 시 `_`(언더스코어) 사용

✅ **예시:**

```java
package org.sopt.buddys.domain.study_management;
```

</details>

<details>
<summary> 📌 클래스명 (Class Naming) </summary>
<br/>

- **파스칼 케이스 (PascalCase)** 사용 (단어의 첫 글자를 대문자로 작성)
- 클래스의 역할을 명확하게 나타내도록 명명

✅ **예시:**

```java
public class StudySessionService { }
```

</details>

<details>
<summary> 📌 인터페이스명 (Interface Naming) </summary>
<br/>

- `I` 접두사 없이 **파스칼 케이스** 사용

✅ **예시:**

```java
public interface EventListener { }
```

</details>

<details>
<summary> 📌 Enum명 (Enum Naming) </summary>
<br/>

- **파스칼 케이스** 사용
- 내부 값은 **대문자 스네이크 케이스** 사용

✅ **예시:**

```java
public enum StudyStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED
}
```

</details>

<details>
<summary> 📌 DTO (Request/Response) </summary>
<br/>

- **파스칼 케이스** 사용
- `Request`, `Response` 등의 접미사 추가

✅ **예시:**

```java
public record StudySessionRequest(String name, int duration) { }
public record StudySessionResponse(Long id, String name, int duration) { }
```

</details>

<details>
<summary> 📌 예외 클래스 (Exception Naming) </summary>
<br/>

- `Exception` 접미사 추가

✅ **예시:**

```java
public class InvalidStudySessionException extends RuntimeException { }
```

</details>

<details>
<summary> 📌 메서드 네이밍 </summary>
<br/>

**🟢 데이터 조회**

- `get~`: 단순 조회
- `find~`: 조건에 따른 조회
- `fetch~`: 외부 시스템에서 가져오기
- `load~`: 내부 로딩, 상태 업데이트

**🔵 상태 확인**

- `is~`: 해당 변수의 상태/속성 확인
- `has~`: 소유/존재 여부 확인
- `can~`: 수행 가능 여부 확인

**🟡 데이터 변경**

- `set~`: 필드나 값의 수정/설정
- `add~`: 컬렉션이나 리스트 등에 새 항목 추가
- `update~`: 기존 데이터를 갱신/수정
- `create~`: 새 객체 생성
- `save~`: 생성 또는 업데이트한 데이터를 영속 저장
- `register~`: 시스템에 등록/회원가입 등의 등록 처리
- `remove~`, `delete~`: 객체나 항목을 삭제/제거
- `replace~`: 기존 항목을 새것으로 교체

**🟣 초기화/변환**

- `init~`: 객체나 컨텍스트를 초기 설정/초기화
- `reset~`: 상태나 데이터를 기본(초기) 상태로 재설정
- `convert~`: 값을 다른 형식/타입으로 변환
- `to~`: 객체를 다른 타입으로 매핑/변환하여 리턴

</details>

<br />

### 🪾 Git Flow

#### 🌿 브랜치 전략

**GIT-FLOW 전략**을 따릅니다.

| 브랜치 | 역할 | 설명 |
| :--- | :--- | :--- |
| **main** | Production | 기준이 되는 브랜치로 제품을 배포하는 브랜치 |
| **dev** | Development | 개발 브랜치로 개발자들이 이 브랜치를 기준으로 각자 작업한 기능들을 Merge |
| **feature** | Feature | 단위 기능을 개발하는 브랜치로 기능 개발이 완료되면 dev 브랜치에 Merge |
| **release** | Release | 배포를 위해 main 브랜치로 보내기 전에 먼저 QA(품질검사)를 하기 위한 브랜치 |
| **hotfix** | Hotfix | main 브랜치로 배포를 했는데 버그가 생겼을 때 긴급 수정하는 브랜치 |

브랜치명은 `feat/#{이슈번호}/{구현할 기능}` 형식으로 작성합니다.

<br />

#### 📝 Git Commit 컨벤션

| 커밋 유형 | 의미 |
| --- | --- |
| feat | 새로운 기능을 추가 |
| fix | 버그 수정 |
| !HOTFIX | 급하게 치명적인 버그를 고쳐야 하는 경우 |
| style | 코드 포맷 변경, 세미콜론 누락 등 코드 수정이 없는 경우 |
| refactor | 프로덕션 코드 리팩토링 |
| comment | 필요한 주석 추가 및 변경 |
| docs | 문서 수정 |
| test | 테스트 코드 추가, 리팩토링 테스트 코드 추가 (Production Code 변경 없음) |
| chore | 빌드 업무 수정, 패키지 매니저 수정, 패키지 관리자 구성 등 업데이트 (Production Code 변경 없음) |
| rename | 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우 |
| remove | 파일을 삭제하는 작업만 수행한 경우 |

- 커밋은 의미 단위로 나누고, 커밋 메시지는 "왜" 변경했는지 알 수 있게 작성합니다.

<br />
<br />

## ‼️ Ground Rule

- 다른 의견이 있을 때는 타당한 근거와 함께 정중하게 말하기 ♡
- 망설이지 말고 질문하기 ♡
- 둥글게 얘기하기 ♡