# Release 0.10.1

0.10.0의 C 디자인과 기능을 유지한 release 서명 빌드입니다. versionCode 18이며, phone·vehicle·demo 세 flavor를 별도 release 키로 서명했습니다. release의 debuggable은 false이고 코드 축소는 사용하지 않습니다.

## 제출 파일

- 차량용: `apk/release/박경태_챙겨가_release.apk`
- 휴대폰용: `apk/release/박경태_챙겨가준비_release.apk`
- 차량 화면 시연: `apk/release/박경태_챙겨가시연_release.apk`

공개 저장소에는 서명된 APK와 소스만 포함합니다. 서명키와 비밀번호는 로컬 비공개 설정에 보관합니다. 파일별 해시는 `apk/SHA256SUMS.txt`에 있습니다.

## 확인

세 flavor의 release 빌드, release 휴대폰 단위 테스트와 lintVital을 통과했습니다. 세 APK의 v2 서명, 동일한 release 인증서, 버전과 비디버그 설정을 확인했습니다.

별도 Android 35 에뮬레이터에 phone·demo release APK를 새로 설치했습니다. 미적재 진행 차단, 두 방문지 적재 후 전송 단계 진행, 위치 권한 허용 후 지도 열기, 공유 텍스트 저장, 차량 시연 앱 전달 완료와 모의 주행 잠금을 실제 조작해 확인했습니다. 실제 차량 신호가 없는 환경의 차량 UI 확인에는 demo flavor를 사용했습니다.

## 기존 앱에서 전환

debug와 release는 서명이 달라 바로 덮어 설치할 수 없습니다. debug 앱에서 목록을 ZIP으로 내보내고 삭제한 뒤 release 앱을 설치해 가져옵니다. 이후 release 업데이트에는 같은 서명키를 사용합니다.
