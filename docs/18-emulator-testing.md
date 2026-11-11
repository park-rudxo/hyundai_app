# 에뮬레이터 실행 검증

2026-11-11 · 휴대폰 0.6.2

## 실행 환경

- Windows WHPX, Android 35 Google APIs x86_64 시스템 이미지 revision 9.
- 화면 1080×2400, density 420, RAM 2GB. CPU는 1코어로 실행.
- Google 공식 이미지의 encryptionkey.img가 포함된 새 가상 기기를 사용했다. 이전 Android 33 이미지는 metadata 파티션 대기에서 부팅이 멈췄고, 4코어에서는 TSC 동기화 오류도 관찰됐다.
- 위치는 실제 사용자 위치 대신 서울시청 테스트 좌표를 주입했다. 위치 권한은 테스트 기기에만 부여했다.

## 직접 화면에서 확인

- 0.6.1 지도 진입 및 네이버 지도 타일 표시, 지도 탭으로 위치 선택.
- 위치 저장 시 별명 선택 대화상자 표시, 별명 없이 저장 후 방문지 목록 복귀.
- 지도 화면의 상단 시스템 바 겹침 없음.
- 메인 화면의 상태바 아이콘 색과 표시 버전 오류를 발견해 0.6.2에서 수정.

## 빌드 재현성

설치 APK의 manifest 버전이 0.6.1인데 화면에는 0.6.0이 표시되는 현상을 확인했다. 버전 표시는 PackageManager의 설치 정보로 바꾸고, Kotlin 증분 컴파일을 끈 뒤 clean 빌드했다. 이전 지도 설정 대기의 원인이 같은 문제였는지는 확정하지 않는다.

## 검증 범위

에뮬레이터의 테스트 위치·가상 렌더러에서 확인한 결과다. 실기기 GPS 수신, 실제 카메라, 차량과의 Wi-Fi 전송, 양산차 Pleos 내비 연결까지 검증한 것은 아니다. 주소 검색은 Android Geocoder이며 네이버 앱의 업체명 검색을 제공하지 않는다.

## 최종 0.6.2 자동 테스트 결과

- JVM 단위 테스트 11개 통과.
- 실제 Android 35 계측 실행: `MapScreenTest` 1개 + `TripFlowTest` 6개, `OK (7 tests)`, 75.91초. 차량 전용 두 메서드는 phone에서 조기 반환하므로 차량 기능 검증 결과로 해석하지 않는다.
- 지도 진입, 표시 버전 일치, 단계 직접 이동, 음성 기능 접근, 적재 전 다음 단계 비활성화, 전달 화면 전환, 저장/사진/ZIP 처리를 검증했다.
- 테스트 중 앱 crash buffer에 종료 로그 없음.

### 실행 명령

```powershell
adb -s emulator-5554 shell am instrument -w -e class io.changyeoga.app.MapScreenTest,io.changyeoga.app.TripFlowTest io.changyeoga.app.phone.test/androidx.test.runner.AndroidJUnitRunner
```

[0.6.2 화면](screenshots/phone-0.6.2-main.png) · [지도 선택 화면](screenshots/phone-0.6.2-map.png) · [별명 없이 저장 후 목록 (0.6.1)](screenshots/phone-map-saved-0.6.1.png)
