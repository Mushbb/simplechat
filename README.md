# 💬 Simplechat: 실시간 채팅 애플리케이션

[![Documentation](https://img.shields.io/badge/📖-Documentation-blue)](https://docs.google.com/document/d/1wFSEGLOJDPE7rZMWJngHO_2RN_8aDOmxuXzHC1BKLjs) [![Live Demo](https://img.shields.io/badge/🚀-Live%20Demo-brightgreen)](https://simplechat-kopo307-app-bzeyf7e4hhcegbhy.koreacentral-01.azurewebsites.net/)

'Simplechat'은 Spring Boot 백엔드와 React 프론트엔드로 구성된 실시간 채팅 애플리케이션입니다. 사용자 간의 원활한 커뮤니케이션을 위한 다양한 기능을 제공하며, 모던 웹 아키텍처를 기반으로 확장성과 유지보수성을 고려하여 설계되었습니다.

## 🔗 바로가기 (Quick Links)

*   **라이브 데모 (Live Demo):** [애플리케이션 바로가기](https://simplechat-kopo307-app-bzeyf7e4hhcegbhy.koreacentral-01.azurewebsites.net/)
*   **프로젝트 명세서 (Docs):** [상세 명세서 보기](https://docs.google.com/document/d/1wFSEGLOJDPE7rZMWJngHO_2RN_8aDOmxuXzHC1BKLjs)

## 🚀 기술 스택 (Tech Stack)

본 프로젝트는 백엔드와 프론트엔드가 분리된 모던 웹 아키텍처로 구성되었습니다.

*   **백엔드 (Backend):**
    *   **언어:** Java 21
    *   **프레임워크:** Spring Boot 3.5.0
    *   **데이터베이스 연동:** Spring Data JDBC
    *   **실시간 통신:** Spring WebSocket + STOMP
    *   **빌드 도구:** Gradle
    *   **주요 라이브러리:** `Jsoup` (링크 프리뷰), `Lombok` (코드 간소화), `Spring Boot Validation` (데이터 유효성 검사)
    *   **빌드 특징:** `node-gradle` 플러그인을 사용하여 Gradle 빌드 시 React 프론트엔드 빌드를 통합 실행
*   **프론트엔드 (Frontend):**
    *   **핵심 기술:** React.js, JavaScript (ES6+), HTML, CSS
    *   **상태 관리:** React Context API
    *   **통신 라이브러리:** Axios, Stomp.js, SockJS-client
    *   **UI 라이브러리:** Bootstrap, React-Bootstrap
    *   **기타:** `react-icons`
*   **데이터베이스 (Database):**
    *   MS SQL Server (MSSQL)
*   **기타 도구:**
    *   **버전 관리:** Git & GitHub
    *   **API 테스트:** Postman 또는 Insomnia

## ✨ 주요 기능 (Features)

### 1. 실시간 채팅 기능
*   **실시간 메시지 송수신:** 텍스트 메시지 전송 및 수신, 실시간 업데이트
*   **메시지 로딩:** 입장 시 최신 메시지 로드 및 스크롤 기반 과거 메시지 로드
*   **파일 업로드:** 사진, 동영상, 일반 파일 업로드 및 공유
*   **링크 프리뷰:** 메시지 내 링크에 대한 미리보기 정보 표시
*   **메시지 멘션:** `@닉네임` 형식으로 특정 사용자 멘션 및 하이라이트 기능
*   **메시지 수정/삭제:** 본인이 보낸 메시지 또는 관리자 권한으로 메시지 수정 및 삭제
*   **닉네임 설정:** 채팅방 내에서 사용자 닉네임 변경 기능

### 2. 채팅방 관리 (로비)
*   **채팅방 목록:** 현재 활성화된 채팅방 목록 조회
*   **채팅방 생성/삭제:** 새로운 채팅방 생성 및 방장 권한으로 채팅방 삭제
*   **채팅방 입장/퇴장:** 채팅방 참여 및 나가기
*   **비밀방:** 비밀번호 설정/해제를 통한 공개방/비밀방 운영
*   **사용자 강퇴:** 방장 권한으로 채팅방 사용자 강퇴
*   **사용자 초대:** 다른 사용자를 채팅방으로 초대 (알림 시스템 연동)

### 3. 사용자 및 친구 관리
*   **회원가입/로그인:** 사용자 계정 생성 및 로그인
*   **개인 프로필:** 프로필 이미지, 닉네임, 상태 메시지 설정 및 변경
*   **친구 관리:** 친구 요청, 수락/거절, 친구 목록 조회, 친구 삭제
*   **알림 시스템:** 친구 요청, 채팅방 초대 등 다양한 알림 수신 및 처리

### 4. 관리 및 유지보수
*   **관리자 명령어:** API를 통해 서버 관리 명령어를 실행하여 채팅방 및 사용자 정보 조회 등 관리 기능 수행
*   **오래된 파일 자동 삭제:** 스케줄링을 통해 주기적으로 오래된 업로드 파일들을 자동으로 삭제하여 서버 자원 관리

## 🔮 향후 개선 과제 (Future Plans)

*   **현재 미구현 기능:**
    *   메시지 전달, 답장, 반응 추가 기능
    *   메시지 검색 기능
    *   DM (Direct Message) 기능
    *   자동 로그인 옵션

*   **신규 위젯/기능 개발:**
    *   채팅 형식 기반 게임 (예: 캐치마인드, 퀴즈)
    *   UI/UX 개선 (색상 테마, 로컬라이제이션 등)
