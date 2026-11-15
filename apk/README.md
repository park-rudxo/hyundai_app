# 챙겨가 APK

제출용 **0.10.1 release (versionCode 18)**입니다. 기존 0.10.0 C 디자인과 기능을 유지하고 별도 release 키로 서명했습니다.

| 앱 | 다운로드 | 용도 |
|---|---|---|
| 차량용 · 주 제출 파일 | [박경태_챙겨가_release.apk](release/박경태_챙겨가_release.apk) | Pleos/AAOS의 실제 P단·UX 제한 확인, NaviHelper 연결 |
| 휴대폰 보조 앱 | [박경태_챙겨가준비_release.apk](release/박경태_챙겨가준비_release.apk) | 지도 검색·준비물 사진·적재·공유 메모·캘린더 작성 |
| 일반 Android 차량 시연 | [박경태_챙겨가시연_release.apk](release/박경태_챙겨가시연_release.apk) | 실제 차량 신호 대신 모의 P단으로 차량 UI 확인 |

[SHA-256](SHA256SUMS.txt) · [시연 영상](../docs/media/changyeoga-demo-0.10.0.mp4) · [설치·연결·빌드](../android/README.md)

## 설치

기존 debug 앱과는 서명이 다릅니다. 목록과 사진을 보관하려면 먼저 ‘목록 내보내기’로 ZIP을 저장하고 debug 앱을 삭제한 뒤 release 앱을 설치해 가져옵니다. 이후 같은 release 키로 서명한 버전은 업데이트 설치합니다.

차량 APK는 Pleos/AAOS 환경에서 실행합니다. 일반 Android에서 차량 화면을 보려면 시연용 APK를 사용합니다. 장소 검색에는 인터넷이 필요합니다.

## 기능

지도·장소명 검색과 별명, 짐 사진 등록, 적재·전달 확인, Wi-Fi·ZIP 전송, 가스레인지 확인 기록, 텍스트 공유 수신, 최근 메모와 Google 캘린더 일정 작성 연결을 제공합니다. 반복 준비물과 백그라운드 자동 알림은 다음 개발 항목입니다.

<details>
<summary>이전 debug 개발 빌드 0.10.0</summary>

기존 개발 앱을 유지할 때 사용하는 파일입니다. 제출용 release 파일은 위 목록을 사용합니다.

- [휴대폰 debug](changyeoga-phone-debug.apk)
- [차량 debug](changyeoga-vehicle-debug.apk)
- [시연 debug](changyeoga-demo-debug.apk)

</details>
