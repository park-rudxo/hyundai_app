# 휴대폰 네이버 지도 · 차량 Pleos 내비 연결

2026.11.8 · 0.5.0 개발 버전. 휴대폰은 네이버 지도로 방문지 좌표를 선택하고, 차량은 Pleos 기본 내비를 호출한다. 2026.11.11 Client ID를 로컬 설정에 적용했다. 실제 네이버 지도 표시 검증은 기기에서 진행해야 한다.

## 먼저 필요한 설정: 네이버 Client ID 발급

1. [네이버 클라우드 콘솔](https://console.ncloud.com)에 본인 계정으로 로그인한다.
2. `Services → Application Services → Maps → Application 등록`으로 이동한다.
3. 공식 Android SDK 가이드에 따라 **Dynamic Map**을 활성화한다.
4. Android 앱 패키지 이름을 **`io.changyeoga.app.phone`**으로 등록한다.
5. 생성한 Application의 인증 정보에서 **Client ID(Key ID)**를 확인한다. Client Secret은 이 앱에서 사용하지 않으며 저장소나 채팅에 올릴 필요가 없다.
6. `android/local.properties`에 아래 한 줄을 추가한다. 이 파일은 Git에서 제외되어 있다. 기존 `sdk.dir`이 있으면 그대로 둔다.

```properties
NAVER_MAP_CLIENT_ID=발급받은_Client_ID
```

환경변수 `NAVER_MAP_CLIENT_ID`도 지원한다. 로컬 파일 설정이 우선한다. Client ID는 모바일 SDK에 포함되는 식별자이며 APK에서 완전히 숨길 수 있는 비밀값이 아니다. 콘솔에 올바른 앱 패키지를 등록한다.

설정 후 **APK를 다시 빌드해야 한다.** 2026.11.11 배포 휴대폰 APK에는 Client ID를 적용했다. 키가 없는 환경에서 재빌드하면 지도 대신 설정 대기 안내를 표시한다. APK를 설치한 다음 휴대폰에서 키를 입력하는 방식은 제공하지 않는다. 서비스 이용 조건·요금·한도는 본인 콘솔에서 확인한다.

발급 항목의 명칭이 다르면 [공식 SDK 시작 가이드](https://navermaps.github.io/android-map-sdk/guide-ko/1.html)를 기준으로 확인한다.

## 사용 흐름

| 기기 | 조작 | 결과 |
|---|---|---|
| 휴대폰 | 방문지 정하기 → 방문지 선택 → `지도에서 위치 선택` | 챙겨가 안에 네이버 지도 표시 |
| 휴대폰 | 지도 탭 또는 주소 검색 결과 선택 → 위치 저장 | 해당 방문지 ID에 위도·경도 저장. 취소하면 기존 위치 유지 |
| 휴대폰 | 지도 현재 위치 버튼 | 위치 권한 요청 후 내 위치 보기. 현재 위치를 방문지로 자동 저장하지 않음 |
| 휴대폰→차량 | 기존 일회 전송 | 사진·상태와 방문지 좌표를 함께 전달 |
| 차량 | `Pleos 지도 열기` | NaviHelper의 NavigationScreenRequest.Foreground로 기본 내비 화면 요청 |
| 차량 | `이 방문지로 길 안내` | 선택한 좌표·방문지 이름으로 requestRoute 호출. 기존 내비 경로를 바꿀 수 있음 |

휴대폰의 주소 검색은 **Android Geocoder**를 사용한다. 네이버 지도 앱의 업체명·POI 검색을 제공하는 것이 아니다. 기기·네트워크에 따라 검색 결과가 없을 수 있으며 지도에서 직접 선택할 수 있다. 검색 결과는 목록에서 사용자가 선택한다.

차량 기능은 실제 vehicle 빌드에만 포함한다. 휴대폰과 demo 빌드에서는 Pleos 내비가 동작한다고 표시하지 않는다. P단·UX 제한 허용 및 NaviHelper 연결 성공 후 버튼을 사용할 수 있다. 호출 성공과 경로 탐색 완료를 구분하고 onRoutingCompleted·onError를 표시한다. 차량 내 지도는 챙겨가 내부에 임베드하지 않고 **Pleos 기본 내비 화면을 연다.** 자동 도착·짐 자동 완료 기능은 이번 구현에 포함하지 않는다.

## 검증과 남은 확인

지도 SDK 의존성 해결과 휴대폰·차량 컴파일을 확인했다. 실제 지도 인증·타일 표시·현재 위치·주소 결과 선택·좌표 저장·재실행은 Client ID 발급 후 시험해야 한다. Pleos 기본 내비 연결·화면 전환·경로 탐색 응답도 실제 지원 AVD에서 미검증이다. 이전 AVD 계측 통과 기록을 이번 지도 동작 검증으로 대체하지 않는다.

1. 휴대폰: 지도 표시 → 주소 검색/지도 탭 → 저장 → 재실행 시 좌표 유지.
2. 휴대폰: 위치 권한 거부, 검색 실패, 지도 취소, 화면 회전 확인.
3. 차량: 전송된 방문지 선택 → 지도 열기 → 길 안내 → 실제 응답 확인.
4. 차량: 서비스 없음·권한 거부·좌표 없음·주행 제한 상태에서 오작동하지 않는지 확인.

## 공식 근거

- [NAVER SDK 시작·인증·공식 Maven](https://navermaps.github.io/android-map-sdk/guide-ko/1.html), SDK 3.24.0.
- [NAVER 지도 객체와 생명주기](https://navermaps.github.io/android-map-sdk/guide-ko/2-1.html). MapFragment 사용.
- [Pleos 공식 NaviHelper 샘플](https://github.com/Pleos-Playground/pleos-sample-navi-app), SDK 3.0.5와 공식 Maven 사용. API 이름·타입은 공식 배포 AAR에서도 확인했다.
- [NaviHelper 목적지 가이드](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations).
