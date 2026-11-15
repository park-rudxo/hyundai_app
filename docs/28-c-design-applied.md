# C 디자인 실제 앱 적용 0.10.0

휴대폰·차량·시연 APK 모두 versionCode 17, versionName 0.10.0입니다.

- #FAFAF8 배경, #30383E 본문, #426A92 행동, #E5E7E7 구분선.
- Pretendard Regular로 제목·본문·행동 통일. 큰 채움 버튼과 카드 제거.
- 폰: 단계별 화면, 간결한 목록, 오른쪽 하단 글자·화살표 다음 행동.
- 차량: 왼쪽 방문지, 오른쪽 준비물, 사이드 영역의 Pleos 내비 동작.
- 사진: 작은 썸네일, 눌러 확대. 사진 없는 빈 상자는 표시하지 않음.
- 지도·별명·공유 메모·캘린더 작성·시작 로딩·연결 창을 같은 스타일로 통일.
- 준비물 상태·전송·P단/UX 제한 판정과 기기 내 저장 방식 유지.

## 확인

휴대폰·차량·시연 빌드 및 휴대폰 단위 테스트 통과. Android 35 에뮬레이터에서 단계 이동·미적재 진행 차단·지도 열기 3개, 차량 전달·취소·재실행과 모의 주행 잠금 2개 UI 테스트 통과. 지도 장소 검색·별명 저장·사진 목록·적재·가스레인지 확인·ZIP 전달·차량 전달 완료·모의 주행 잠금을 실제 조작하여 새 영상을 촬영했습니다.

시연 영상의 차량 상태는 demo 앱의 모의 입력입니다. 차량 APK는 기존 CarPropertyManager/CarUxRestrictionsManager와 Pleos NaviHelper 연결을 사용합니다.

## 실행 화면

[폰](screenshots/c-0.10.0/phone.png) · [차량](screenshots/c-0.10.0/vehicle.png) · [지도](screenshots/c-0.10.0/map.png)

[APK](../apk/README.md) · [디자인 기준](design/DESIGN-SYSTEM.md)
