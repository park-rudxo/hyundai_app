# 챙겨가 Android 앱

현재 버전은 **0.10.0 (versionCode 17)**입니다. Kotlin·Jetpack Compose로 휴대폰·Pleos 차량·차량 화면 시연 앱을 하나의 프로젝트에서 빌드합니다.

## 실행할 앱 고르기

| flavor | 패키지 | 역할 |
|---|---|---|
| phone | io.changyeoga.app.phone | 네이버 지도·장소명 검색, 별명, 사진 준비물, 적재·전달 확인, 가스레인지 확인, 공유 메모와 캘린더 작성 |
| vehicle | io.changyeoga.app | 실제 P단·UX 제한을 확인하는 Pleos/AAOS 차량 앱. NaviHelper로 기본 내비와 방문지 경로 요청 |
| demo | io.changyeoga.app.demo | 일반 Android에서 차량 UI를 보여주는 시연 앱. 실제 차량 신호 대신 모의 P단 사용 |

[APK 다운로드](../apk/README.md) · [실제 앱 영상](../docs/media/changyeoga-demo-0.10.0.mp4)

## 사용자 흐름

1. 휴대폰에서 네이버 지도로 방문지를 찾습니다. 장소명을 입력하면 결과가 나오며, 선택 후 위치와 별명을 저장합니다.
2. 방문지마다 가져갈 물건 사진을 등록합니다. 사진은 작은 썸네일로 표시하고 누르면 확대합니다.
3. 차에 넣은 물건에 ‘실었어요’를 누릅니다. 출발 전 집안 확인에서 가스레인지 확인 시각도 남길 수 있습니다.
4. 차량 연결 단계에서 목록을 전송합니다. 차량에서는 왼쪽 방문지와 오른쪽 준비물 목록을 확인합니다.
5. 주차 후 물건을 건네고 ‘건넸어요’를 누릅니다. ‘적재 취소’와 ‘전달 취소’로 상태를 되돌릴 수 있습니다.

방문지마다 준비물이 있어야 적재 단계로 진행하고, 모든 준비물의 적재를 확인해야 전송 단계로 진행합니다. 목록과 사진은 기기에 저장합니다. ‘새 외출’은 확인 대화상자 후 현재 목록과 사진을 초기화합니다.

상단 ‘파일·새 외출’ 또는 차량 ‘더보기’에서 ZIP 가져오기·내보내기와 새 외출을 선택합니다. 다른 앱의 텍스트 공유는 ‘공유받은 글 확인’ 화면으로 연결합니다. 공유 메모는 최근 1개를 보관하며, 캘린더 일정은 Google 캘린더의 작성 화면에서 사용자가 저장합니다.

## 연결과 저장

### 같은 Wi-Fi로 보내기

1. 폰과 차량을 같은 Wi-Fi 또는 핫스팟에 연결합니다. 차량은 P단이어야 합니다.
2. 폰에서 ‘차량으로 보내기 → 보내기 시작’을 누르고 화면을 열어 둡니다.
3. 차량에서 ‘휴대폰에서 받기’를 열고 폰의 주소와 포트를 입력합니다.
4. ‘휴대폰 확인’ 후 두 화면의 확인 문구를 비교하고 연결 코드 6자리를 입력합니다.
5. ‘문구가 같아요 · 받아서 교체’를 누릅니다.

전송 시점의 방문지·사진·상태를 한 번 보내며 차량의 기존 목록을 교체합니다. 수정 후에는 다시 전송합니다. TLS 연결을 사용하며 사진과 목록은 합계 20MB까지입니다. 세션은 5분, 성공 1회 또는 잘못된 코드 5회까지 유효합니다. 폰이 보내기 화면을 나가면 연결을 종료합니다.

### ZIP으로 옮기기

폰의 ‘파일·새 외출 → 목록 내보내기’에서 ZIP을 저장합니다. 파일을 차량으로 옮긴 뒤 차량 ‘더보기 → 목록 가져오기’에서 선택합니다. 시연 영상은 이 방식으로 같은 목록을 전달했습니다.

## 빌드 환경과 지도 설정

JDK 17, Android SDK Platform 35, Gradle wrapper 8.10.2를 사용합니다. minSdk 26, compile/targetSdk 35, Kotlin 2.0.0, AGP 8.8.2입니다. 차량용 실행에는 Android Automotive의 android.car 서비스가 필요합니다.

`android/local.properties`에 개발 PC의 SDK 경로와 지도 설정을 입력합니다. 이 파일은 Git에서 제외됩니다.

```properties
sdk.dir=C:/Users/YOUR_USER/AppData/Local/Android/Sdk
NAVER_MAP_CLIENT_ID=YOUR_MAPS_CLIENT_ID
PLACE_SEARCH_URL=https://hyundai-app-xc7k.onrender.com
```

Maps Client ID는 네이버 지도용이며 Android 패키지 `io.changyeoga.app.phone`을 등록합니다. 장소명 검색용 API HUB 인증정보는 [검색 서버](../server/README.md)에서 관리합니다. Client Secret을 앱 설정에 넣지 않습니다. 다른 서버로 빌드하려면 `PLACE_SEARCH_URL`을 바꾸고 `/places` 없이 기본 주소만 입력합니다. 같은 이름의 환경변수로도 설정할 수 있습니다.

### Windows PowerShell

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA/Android/Sdk"
cd android
.\build.ps1
```

빌드 결과는 `%LOCALAPPDATA%/Changyeoga/build/app/outputs/apk/<flavor>/debug/`에 생성합니다. 한글 경로에서 테스트 JVM의 클래스 경로 문제를 피하도록 출력 경로를 따로 사용합니다. 다른 출력 경로는 다음처럼 지정합니다.

```powershell
.\gradlew.bat assemblePhoneDebug assembleVehicleDebug assembleDemoDebug testPhoneDebugUnitTest '-PbuildRoot=C:/Temp/changyeoga-build'
```

### 다른 환경

```sh
cd android
sh gradlew assemblePhoneDebug assembleVehicleDebug assembleDemoDebug testPhoneDebugUnitTest
```

출력 경로를 지정하지 않으면 `android/app/build/outputs/apk/<flavor>/debug/`에 생성됩니다. 현재 제공 파일은 debug 서명 빌드이며, release 서명 설정은 별도로 준비합니다.

### 설치

```sh
adb install -r apk/changyeoga-phone-debug.apk
adb shell am start -n io.changyeoga.app.phone/io.changyeoga.app.MainActivity
# 차량용 패키지: io.changyeoga.app
# 시연용 패키지: io.changyeoga.app.demo
```

위 설치 명령은 저장소 루트에서 실행합니다. 기존 목록을 유지하려면 삭제 없이 업데이트 설치합니다.

## SDK와 시험 환경

- 차량: CarPropertyManager의 GEAR_SELECTION과 CarUxRestrictionsManager를 확인합니다. 연결이 끊기거나 주차·UX 제한을 확인할 수 없으면 조작을 잠급니다.
- 내비: Pleos NaviHelper 3.0.5로 기본 내비 화면과 방문지 경로를 요청합니다.
- 지도: 네이버 지도 SDK 3.24.0과 Play Services Location을 사용합니다. 장소명 검색은 API HUB 중계 서버를 호출합니다.
- 음성 보조 도구: 현재 Android 음성 인식 앱과 TTS를 호출합니다. Pleos SpeechToText 연동은 다음 개발 항목입니다.

| 시험 구분 | 환경 |
|---|---|
| 초기 Pleos SDK·차량 신호 | Pleos Connect 2.1, Android 14/API 34, x86_64, RAM 4GB |
| 최신 폰 UI·자동 시험 | Android 15/API 35, 1080×2400, density 420 |
| 최신 차량 시연 UI | Android 15/API 35, 1920×1080, density 240, demo 앱의 모의 상태 |

0.10.0에서 세 flavor 빌드와 휴대폰 단위 테스트, 휴대폰 UI 3개·차량 시연 UI 2개 검사를 통과했습니다. [적용·실행 기록](../docs/28-c-design-applied.md)과 [초기 Pleos 기록](../docs/05-first-run.md)을 참고하세요.

## 다음 개발

반복 준비물, 출발·도착 시점의 자동 안내와 백그라운드 알림, 캘린더 일정 불러오기, 공유받은 텍스트·이미지의 일정 및 준비물 등록을 개발합니다. 현재 차량 방문지는 직접 선택하며, 캘린더 연결은 일정 작성 화면 호출입니다. 지속 동기화와 준비물 자동 분석은 제공하지 않습니다.

[디자인 기준](../docs/design/DESIGN-SYSTEM.md) · [공식 Pleos 개발 문서](https://document.pleos.ai/) · [프로젝트 개요](../README.md)
