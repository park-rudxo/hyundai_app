# 사용 흐름 개선 작업

공식 참고 자료:
- Android 에이전트 스킬: https://github.com/android/skills
- Compose 공식 샘플: https://github.com/android/compose-samples
- Material 3: https://developer.android.com/develop/ui/compose/designsystems/material3
- 백그라운드 위치: https://developer.android.com/develop/sensors-and-location/location/background
- 지오펜스: https://developer.android.com/develop/sensors-and-location/location/geofencing

외부 스킬은 설치하지 않았다. 공식 자료를 우선 참고한다. 차량 연결은 기본 흐름으로 유지한다. 중계 서버가 없으므로 검색 서버 코드를 먼저 준비하며, 검색 인증과 실제 배포는 별도다. 도착 알림은 화면 종료·화면 잠금·프로세스 종료·강제 종료를 구분해 검증해야 한다.
