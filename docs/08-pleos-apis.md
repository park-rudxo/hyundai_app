# Pleos Connect SDK로 알 수 있는 것과 할 수 있는 일

2026-11-03 · 차량 안에서 구동하는 Android 앱 기준. SDK는 개발 도구 묶음, API는 앱이 호출하거나 이벤트를 받는 개별 기능이다. 아래 상태는 공식 프로젝트 신청 가이드 기준이며 실제 프로젝트 콘솔과 차종·버전별 지원 여부는 별도 확인한다.

| 모듈 | 개발 문서가 제공하는 기능 | 신청 가이드 / 이 프로젝트에서 확인한 범위 |
|---|---|---|
| Vehicle | 도어, 좌석, 공조, 조명, EV 배터리, 연료, 타이어 등 영역별 상태·지원 범위 조회. 기능에 따라 변경 알림 구독과 제어 API도 있다. | **Vehicle State**는 별도 신청 없이 가능하다고 안내. **Vehicle Control**은 준비 중으로 표기. 로컬에서는 운전석 앞문 읽기만 확인했다. |
| NaviHelper | 목적지·경유지 설정과 조회, 경로 재탐색, 현재 위치와 주행 정보 조회, 목적지 도착·경로 변경 이벤트. | 신청 없이 가능하다고 안내. v3.0.5 문서 기준이며 AVD에서 이벤트와 반환 필드는 아직 미검증. 경로 재탐색 이벤트가 지도에 그릴 좌표열까지 제공한다는 뜻은 아니다. |
| Fused Location | 차량 위치를 제공하는 전용 모듈. 내비 서비스의 현재 위치 조회와 진입점이 다르다. | 신청 가이드상 준비 중. 연속 좌표, 백그라운드 추적, 실내 정밀 위치를 출품 앱의 전제로 두지 않는다. |
| TextToSpeech | 앱의 문장을 음성으로 합성한다. | 신청 가이드상 준비 중. 프로젝트 클라이언트 정보가 쓰인다. 기기 기본 Android TTS 사용 실험은 별도 구분한다. |
| SpeechToText | 발화를 텍스트로 인식한다. | 신청 가이드상 준비 중. 화자 식별·좌석 판별·차 안 상시 청취는 별개 기능으로, 지원을 확인하지 못했다. |
| ADAS | 주행 보조 정보를 제공한다. 레퍼런스에 로컬 지도 차선 데이터 변경 콜백 등이 있다. | 공모전 일반 계정의 신청·접근 범위는 미확인. 카메라 원본 영상이나 조향·제동 권한을 의미하지 않는다. |

**차량 앱과 서버 API의 경계.** 별도 Vehicle Data API에는 차량 식별번호와 인증 토큰 등으로 배터리 충전 상태를 조회하는 서버 엔드포인트도 있다. 이 응답 예시를 차량 내 Vehicle SDK가 그대로 반환한다고 인용하면 안 된다. 충전기 사업자 결제, 충전소의 휠체어 접근 공간, 다른 사람 휴대폰의 음원 등은 위 표에서 확인한 입력이 아니다.

## 기술 활용성 심사용 기록

이미 확인한 흐름은 Vehicle SDK 2.3.2 → Door Position 지원 범위 확인 → ROW_1_LEFT 상태 조회 → AVD 화면의 값 0(닫힘)이다. Service는 2.3.22였다. 다른 신호도 기능별 capability, 권한, 실제 값, 사용자 화면 반응을 각각 남긴다. null·미지원·권한 거부도 결과로 기록한다. AVD 실측과 양산차 지원, 앱 마켓 승인 범위는 구분한다.

## 원본과 출처

- [공식 SDK 개요 · 6개 모듈과 권한 정책](https://document.pleos.ai/) · [프로젝트 생성 및 SDK 사용 신청 · 모듈별 상태](https://document.pleos.ai/docs/pleos-only/create-my-project)
- [Vehicle 도메인 및 조회·제어 구조](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/intro) · [공식 Vehicle 샘플](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app) · [로컬 실측 및 캡처](05-first-run.md)
- [NaviHelper 개요](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/intro) · [내비 이벤트 목록](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/event-listeners)
- [ADAS 차선 데이터 콜백](https://document.pleos.ai/api-reference/connect-sdk-pleos/Adas/adas/ai.pleos.playground.adas.observe/subscribe-local-map/on-lane-data-change) · [별도 서버 Vehicle Data API의 충전 상태 조회 예시](https://document.pleos.ai/en/api-reference/vehicle-data-api/vehicle-data-api/common/get-battery-charging-status)
