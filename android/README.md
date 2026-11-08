# 챙겨가 Android 첫 실행 버전

사진으로 등록한 짐을 방문지별로 나누고 적재와 전달을 별도로 확인하는 Kotlin·Jetpack Compose 앱입니다. 현재 버전은 **같은 네트워크에서 목록을 전달할 수 있는 0.4.0 개발용 앱**입니다.

## 0.4.0 휴대폰 화면 변경

앱을 열면 방문지 정하기 → 짐 사진 등록 → 차에 싣기 → 차량에 보내기 → 도착 후 전달 순서로 진행합니다. 이전/다음 버튼으로 이동하고 현재 단계는 재실행 후 복원합니다. 각 방문지에 사진이 있어야 적재 단계로, 모든 짐의 적재를 확인해야 전송 단계로 넘어갑니다. 차량 전송은 선택 사항입니다. 파일 가져오기·내보내기, 새 외출, 위치·음성 도구는 상단 더보기로 옮겼습니다. 차량용 화면은 기존 구성을 유지합니다.

위치 찾기는 GPS와 네트워크 위치 제공자를 함께 요청합니다. 좌표와 오차를 표시하고 30초 동안 수신이 없으면 설정 확인 방법을 안내합니다. 정확한 위치 권한이 필요하며 자동 도착과 현재 위치 저장은 기존 50m 정확도·15초 이내 조건을 유지합니다. 대략적인 위치 수신과 정밀 위치 확보를 구분합니다. 실제 사용자 휴대폰에서의 수신 개선은 재시험이 필요합니다.

## 실행 버전

| 빌드 | 앱 이름 | 용도 |
|---|---|---|
| phoneDebug | 챙겨가 준비 | 휴대폰에서 방문지 추가, 카메라 촬영 또는 사진 선택, 상태 확인 |
| vehicleDebug | 챙겨가 | AAOS의 실제 P단과 CarUxRestrictions를 읽고 둘 다 허용할 때만 조작 가능 |
| demoDebug | 챙겨가 시연 | 모의 P단 스위치가 있는 개발 시연 전용. 실제 차량 신호를 읽지 않음 |

시연용은 차량용과 패키지가 다릅니다. 실제 차량용에는 모의 잠금 해제 스위치가 없습니다. 세 APK는 디버그 서명이며 공모전 제출용 release가 아닙니다.

## 현재 가능한 흐름

1. `첫 방문지 추가`에서 외가·친가 등 방문지 이름을 입력합니다. 내비 목적지 연결은 아직 없습니다.
2. 방문지를 선택하고 사진을 추가합니다. 휴대폰용은 외부 카메라 앱의 촬영 미리보기 또는 사진 선택을 사용합니다. 원본 대신 앱 내부에 JPEG 사본을 저장하므로 선택한 사진 파일을 나중에 이동해도 목록이 유지됩니다. 촬영은 현재 저해상도 미리보기이며 고해상도 촬영은 후속 작업입니다.
3. 짐을 실제로 실은 뒤 `실었어요`를 누릅니다.
4. `도착·전달`에서 방문지를 직접 선택하고 `건넸어요`를 누릅니다. 적재 확인 전에는 전달 완료로 바꿀 수 없습니다. 도착지를 선택하는 것만으로 완료 처리하지 않습니다.
5. `되돌리기`로 직전 상태로 돌아갑니다. 강제 종료·재실행 시 사진, 방문지, 상태를 복원합니다.

사진이 없어도 `예시로 둘러보기`로 두 방문지와 짐의 흐름을 확인할 수 있습니다. 예시 항목은 실제 촬영 사진이 없다는 표시를 유지합니다. `새 외출`은 확인 대화상자 후 현재 목록을 초기화합니다.

## 휴대폰에서 차량으로 보내기

1. 휴대폰과 차량을 같은 Wi-Fi 또는 핫스팟에 연결합니다. 차량은 P단이어야 합니다.
2. 휴대폰 앱에서 `차량으로 보내기 → 보내기 시작`을 누르고 화면을 열어 둡니다.
3. 차량 앱에서 `휴대폰에서 받기`를 열고 휴대폰의 `주소:포트`를 입력합니다.
4. `휴대폰 확인` 후 두 화면의 확인 문구가 모두 같은지 비교하고, 휴대폰의 6자리 코드를 입력합니다.
5. `문구가 같아요 · 받아서 교체`를 누르면 방문지·사진·체크 상태를 교체합니다. 기존 차량 목록과 병합하지 않습니다.

시작 시점의 목록을 한 번 전송합니다. 수정 후에는 다시 보내야 합니다. 세션은 5분, 성공 1회 또는 잘못된 코드 5회까지이며 휴대폰 앱을 나가면 종료됩니다. 사진과 목록은 합계 20MB 이하입니다. TLS 1.2와 인증서 확인을 사용하고, 외부 서버에 업로드하지 않습니다.

같은 Wi-Fi여도 공유기의 기기 간 통신 차단, 차량의 네트워크 정책 때문에 연결되지 않을 수 있습니다. 이 경우 기존 `목록 내보내기/가져오기`로 ZIP을 이동할 수 있습니다. **현재 검증은 Pleos AVD 내부 루프백 전송이며 실제 휴대폰↔차량 Wi-Fi 왕복은 아직 확인하지 않았습니다.**

## 휴대폰 GPS와 음성 명령 (0.3.0)

- 휴대폰 앱에서 방문지를 고른 뒤 `휴대폰 GPS 켜기`로 정확한 위치 권한을 허용합니다. `방문지 위치`에 목적지 위도·경도를 입력하거나 **그 목적지에 실제로 있을 때** `현재 GPS 사용 → 위치 저장`을 누릅니다. 주소 검색·지도 선택은 아직 없습니다.
- 이 화면이 켜져 있을 때만 GPS를 구독합니다. 다른 앱으로 이동하거나 화면이 꺼지면 중단되고 돌아오면 다시 시작합니다. 배경 감지·알림 서비스는 아직 없으므로 운전 중 화면 조작 대신 동승자가 시험합니다.
- 정확도 50m 이내, 15초 이내의 위치를 사용합니다. 거리+오차가 150m 이내인 상태가 20초 이어지면 방문지 근처 안내와 해당 짐 화면을 표시합니다. 이는 도착 후보이며 정차나 실제 전달을 증명하지 않습니다. 인접 목적지가 모호하면 자동 선택하지 않습니다. 세션 내 재안내는 500m 밖으로 나간 뒤 재접근할 때 가능합니다.
- 위치는 **휴대폰에서 감지·안내**합니다. 차량에 실시간 위치를 중계하지 않습니다. 저장한 목적지 좌표는 기존 일회 목록 전송에 포함됩니다. 이동 경로는 저장하지 않습니다.
- `음성 명령`은 설치된 Android 음성 인식 앱을 호출합니다. `짐 알려줘`, `짐 읽어줘`, `외가 도착`(등록한 방문지 이름), `출발 준비`를 지원하며 인식된 문장을 확인한 후 실행합니다. 적재·전달 완료는 자동 처리하지 않습니다. 같은 이름이 여러 개면 음성 선택을 거부합니다.
- 음성 인식 앱/한국어 TTS가 없으면 버튼·글 안내를 사용합니다. 음성 인식 제공자에 따라 인터넷 및 제공자의 음성 처리가 필요할 수 있습니다. 상시 듣기나 Pleos 기본 음성 비서 연동은 아닙니다. 차량 버전은 기존 P단·UX 제한이 허용할 때만 명령 UI를 제공합니다.

## 개발 환경과 빌드

JDK 17, Android SDK Platform 35, Android Gradle Plugin 8.8.2, Kotlin 2.0.0, Gradle wrapper 8.10.2를 사용합니다. Android 8(API 26) 이상을 대상으로 합니다. 차량용은 AAOS 및 `android.car` 라이브러리가 있는 환경이 필요합니다.

Windows PowerShell에서:

```powershell
cd android
# JAVA_HOME, ANDROID_HOME이 설정돼 있으면 그 값을 사용합니다.
.\build.ps1
# 연결된 Pleos 에뮬레이터에서 UI 검증
.\build.ps1 -Tasks ':app:connectedDemoDebugAndroidTest'
```

Windows의 한글 프로젝트 경로에서 Gradle 테스트 JVM이 클래스를 찾지 못하는 문제를 피하려고 스크립트는 생성물을 `%LOCALAPPDATA%/Changyeoga/build/app`에 둡니다. 소스는 현재 저장소에 그대로 있습니다. Windows 사용자 폴더도 한글인 경우 `gradlew.bat`에 `-PbuildRoot=C:/영문경로` 형태의 실제 영문 경로를 직접 지정합니다.

영문 경로의 다른 환경에서는:

```sh
sh gradlew :app:assemblePhoneDebug :app:assembleVehicleDebug :app:assembleDemoDebug :app:testDemoDebugUnitTest
```

APK는 `<buildRoot>/app/outputs/apk/<flavor>/debug/`에 생성됩니다. buildRoot를 지정하지 않으면 `app/build/outputs/apk/`입니다.

이 PC에서는 Pleos AVD의 화면 갱신 지연을 관찰했습니다. `-gpu swiftshader_indirect -no-snapshot-load -no-snapshot-save` 옵션으로 재시작한 뒤 정상 표시와 조작을 확인했습니다. 부팅 직후 기본 내비가 전면으로 열릴 수 있으므로 초기 로딩 후 앱을 다시 선택합니다. 자세한 관찰은 개발 기록에 남겼습니다.

```powershell
adb install -r <APK경로>
adb shell am start -n io.changyeoga.app/.MainActivity
# 시연용: io.changyeoga.app.demo/.MainActivity
# 휴대폰용: io.changyeoga.app.phone/.MainActivity
```

## 아직 연결하지 않은 기능

- 지속적인 자동 동기화와 차량→휴대폰 역방향 무선 반영: 현재는 휴대폰→차량 일회 전송 또는 ZIP 이동입니다.
- NaviHelper 목적지·경유지 및 자동 도착 이벤트: 현재 직접 선택합니다.
- 예상 출발 N분 전 예약 알림과 외출 전 점검 사진의 별도 분류.
- 실제 휴대폰 카메라와 양산차 검증, release 서명, 앱마켓 심사.
- 차량용 화면 크기·카메라 품질 등 실제 사용 환경의 추가 조정.

## 원본과 출처

- [Pleos 주행 제한 개발 가이드](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide): CarUxRestrictionsManager 연결 근거.
- [AAOS VehiclePropertyIds](https://developer.android.com/reference/android/car/VehiclePropertyIds): GEAR_SELECTION을 통한 P단 확인. 신호를 읽을 수 없으면 잠급니다.
- [공식 Vehicle 샘플](https://github.com/Pleos-Playground/pleos-sample-vehicle-app/tree/959981d): 검증된 Gradle wrapper와 의존성 버전을 참고했습니다. 앱 구현은 새로 작성했습니다. Gradle wrapper 파일의 Apache 2.0 고지를 유지했습니다.
- [기획서 초안](../docs/15-proposal-draft.md), [검증 기록](../docs/16-development-log.md).

- [Android 위치 권한](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [LocationManager](https://developer.android.com/reference/android/location/LocationManager)
- [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent)
