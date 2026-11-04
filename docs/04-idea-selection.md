# 출품 주제 탐색

## 11월 4일 우선 검토 · 한 번에 적는 방문지별 짐

집에서 외가에 줄 과일과 친가에 줄 선물을 한 번 입력한다. 집에서는 둘 다 실었는지 확인하고, 외가에 도착하면 과일만, 친가에 도착하면 선물만 차량 화면에 띄우는 구상이다. 실었음과 전달했음은 사용자가 따로 확인한다.

| 확인한 서비스 | 실제로 겹치는 부분 | 확인 범위 |
|---|---|---|
| 기아 PBV 배송 솔루션 사례 | 출발 전 차량 화면에서 일정·등록 물품 수를 확인하고 목적지별로 배송 물품 수와 상세 정보를 확인한다. 가장 가까운 차량 선례다. | 업무용 시나리오다. 가족이 직접 입력하는 승용 Pleos 앱의 출시 여부나 주차 순간 자동 팝업까지 보여 주는 자료는 아니다. [기아 공식 사례](https://www.kia.com/kr/pbv/solution/about-solution) |
| Apple CarPlay + Siri 미리 알림 | 차 안에서 ‘집에 도착하면 우산 챙기기’ 같은 요청을 등록할 수 있다. 외가·친가 알림을 각각 만들면 목적지별로 기억할 일을 맡길 수 있다. | 공식 예시는 음성 등록이다. 차량 화면의 다중 경유지 짐 목록까지 확인한 것은 아니다. [Apple 사용 설명서](https://support.apple.com/guide/iphone/use-siri-in-your-car-iph0aa8c80e6/26/ios/26) |
| Memo Push · Pocket Lists | 전자는 체크리스트를 장소에 연결해 떠날 때 미확인 물건을 알린다. 후자는 할 일에 도착·출발 위치 알림을 붙인다. | 출발 준비와 장소 알림 각각은 소비자용 대안이 있다. [Memo Push](https://www.memopush.com/) · [Pocket Lists](https://pocketlists.com/help/26/location-based-reminders/) |
| Geo2 | 배송 기사가 출발 전 여러 정차지와 물품의 적재 위치를 등록하고 도착지별로 짐을 찾아 인도 내역을 남긴다. | 배송 실무용 모바일 서비스다. [Geo2 제품 안내](https://geo2.com/route-planner) |

현대차·42dot의 10월 30일 Gleo 업데이트와 공개 앱마켓 자료에서 이 가족 방문 흐름 전체는 확인하지 못했다. 설치된 실차 앱 전체를 대조한 결과는 아니다. 독창성은 ‘장소 알림’이나 ‘차량에서 목적지별 물품 보기’만으로 주장하기 어렵다. 검증할 차이는 **개인이 물건과 전달처를 한 번만 입력하면 출발 준비와 방문지별 전달 확인을 모두 마칠 수 있는가**다. 출품 결정 전, 두 방문지에 줄 짐을 준비하는 같은 과제를 휴대폰 기본 알림과 시안으로 각각 수행해 입력 횟수·준비 시간·누락을 비교한다.

## 새 후보 · 물건 인계와 차량 오디오 검수

| 후보 | 첫 시연 | 기존 서비스와 남은 질문 |
|---|---|---|
| 카풀 짐 인계 | 출발 전 동승자별 짐을 등록한다. 두 경유지 가운데 체육관에 정차하면 그곳에서 내릴 사람의 짐만 표시하고 직접 인계 확인한다. | GearLog는 대여·반납을 지원한다. 차별화 가설은 여러 정차지에서 사람과 짐을 함께 내리는 순서다. 물건 자동 감지는 전제하지 않는다. 경유지 도착 이벤트 수신과 실제 누락 경험을 확인한다. |
| 내 차 믹스룸 | 자작곡 한 구절의 두 믹스를 같은 차에서 듣는다. 앱은 재생 시점의 정차·주행 조건을 기록하고 주차 후 조건별 청취 메모를 비교한다. | MixShuffle은 이미 차 안에서도 블라인드 비교를 제공한다. 남은 가설은 같은 운행 조건에서 어느 파트가 묻혔는지를 반복 검수하는 가치다. 차속을 소음 측정값으로 해석하지 않는다. |

두 후보 모두 수요·Pleos 입점 중복·필요 신호의 AVD 수신은 미확인이다. [NaviHelper 이벤트와 차속 조회 범위](08-pleos-apis.md)를 참고한다.

## 제외 · 동승자를 위한 주행 습관 음성 코칭

같은 구간을 다시 달리지 않아도 현재 차량 속도에서 반복되는 패턴을 구분하고 운전자에게 한 문장으로 알려준다. 세부 유형과 시연 기준은 [설계 문서](14-motion-sickness.md)에 정리했다. 아래 내용은 선정 전 조사 기록이다.

11월 4일, 동승자의 차멀미를 우선 조사하기로 했다. CDC는 멀미 민감도가 보통 7~12세에 가장 높고 성인이 되면서 낮아진다고 설명한다. GV80 부분변경 모델에는 전방 노면에 맞춰 서스펜션을 제어하는 승차감 기술이 있다. 두 차종에서 같은 사람의 멀미 발생을 비교한 근거는 확보하지 못했다.

현대차·기아 임직원 아이디어 페스티벌에서 2026년 10월 ‘멀미 제로’가 최우수상을 받았다. 실내 조명·후석 화면을 이용한 시제품이다. Apple도 동승자의 휴대폰 화면 가장자리에 차량 움직임을 표시한다. 차별화 가설은 **멀미가 잦은 동승자가 출발 전 어느 구간에서 폰을 내려놓거나 쉴지 미리 정하는 경험**이다. 기존 내비가 주는 안내로 충분한지, Pleos에서 경로상의 연속 굽이 구간을 출발 전에 식별할 수 있는지는 미확인이다. 먼저 최근 실제 이동에서 처음 불편해진 시점과 당시 원했던 도움을 듣는다. 시연으로 측정할 것은 계획을 세우는 데 도움이 되는지이며, 증상을 유발하는 시험은 필요하지 않다.

## 11월 4일 다시 좁힌 후보

육성형 게임은 참가자 본인이 비슷한 앱을 꾸준히 쓰지 않았다는 경험을 반영해 제외했다. 아래 두 가지는 선택 전 가설이다.

| 후보 | 한 번의 탑승에서 얻는 것 | 반복 이용과 첫 검증 |
|---|---|---|
| **도착 전 한 편** · 출퇴근하며 오디오 추리물을 듣는 사람 | 남은 시간에 맞춰 한 사건의 단서를 편집한다. 8분 경로에서는 핵심 단서 두 개와 결말, 15분 경로에서는 추가 증언까지 듣는다. 단서 재생 중 예상 시간이 줄면 다음 장면 경계에서 짧은 판으로 바꾼다. | 다음 출근길에 새 사건을 골라 듣는지 본다. 같은 이야기를 휴대폰에서 회차 길이만 골라 듣는 조건과 비교한다. 직접 녹음한 사건 한 편으로 남은 시간이 바뀌는 순간을 시연한다. NaviHelper의 remainTime은 문서상 확인했고 AVD 수신은 미검증이다. |
| **가는 김에, 내 지도** · 가고 싶은 장소를 저장해 두는 운전자·동승자 | 출발 전에 오늘 쓸 수 있는 여유 시간을 정한다. 저장한 세 장소 중 목적지까지 우회와 체류를 합쳐 그 시간 안에 들를 한 곳을 보여 주고 경유지로 추가한다. | 다음 외출 때 이전에 저장한 장소를 다시 꺼내는지 본다. Google 지도에도 장소 저장과 경로상 우회 시간 비교가 있어 ‘체류까지 포함해 직접 계산할 일이 줄었는가’를 비교한다. Pleos 자체에서 대안별 소요 시간 계산은 미확인이다. 첫 시연은 미리 측정한 한 경로로 범위를 제한하고 예시 시간을 표시한다. |

**검토 결과 · 두 후보 모두 제외.** ‘도착 전 한 편’은 참가자가 다시 쓰고 싶은 주제가 아니었다. ‘가는 김에, 내 지도’는 Pleos 기본 내비와 Gleo가 이미 주변 맛집을 확인하고 경로를 다시 잡는 장면을 지원한다. 현대차의 기존 맛집 로드는 경로·경유지 주변 추천을, Google 지도는 저장과 우회 시간 비교를 제공한다. 개인 저장 목록에서 체류 시간까지 합산해 자동 선택하는 조합은 Pleos 공식 자료에서 확인하지 못했지만, 이 차이로 별도 앱을 다시 열 이유가 충분하지 않다고 판단했다. 최종 주제는 미정이다.

## 이전 후보

2026년 11월 3일. 가족 픽업은 블루링크 위치 공유와 핵심 경험이 겹쳐 제외했다. 타깃은 열어 두고 설명회의 모빌리티·라이프, 엔터테인먼트, 안전·동승자 케어 중 한 분야에서 차량 기능이 실제로 필요한 문제를 찾는다. 아래 제안의 수요와 독창성은 검증 전이다.

| 주제와 대상 | 30초 시연 | 비교할 기존 기능과 첫 검증 |
|---|---|---|
| **캠핑 전력 예산** · 전기차로 캠핑하는 사람 | 현재 배터리와 남겨 둘 충전량을 기준으로 냉장고·전기장판 사용 계획을 표시한다. 장판 시간을 8시간에서 4시간으로 바꾸면 예상 잔량이 달라진다. | 현대차 V2L은 설정한 한도에 도달하면 전력 공급을 중단한다. 사용 전 여러 기기의 조합을 비교하는 데 가치가 있는지 캠퍼에게 묻는다. 초기에는 기기 소비전력과 배터리 용량을 수동 입력하고 SOC 조회를 AVD에서 시험한다. 예상치는 기기의 실제 가동률에 따라 달라진다. |
| **승객이 필요한 하차 공간** · 휠체어로 옮겨 타는 승객과 운전자 | 출발 전에 ‘오른쪽 뒷문 옆에 이동 공간 필요’를 지정한다. 목적지 주차 전에 운전자에게 어느 문을 비워 둘지 한 번 알려준다. | 원격 스마트 주차 보조와 비교한다. 첫 시연은 승객의 요청을 전달하는 데 한정한다. 센서로 문 열 공간이나 안전을 판정하지 않는다. 실제 이용자에게 구두 전달이 실패한 최근 사례가 있는지 먼저 묻는다. |
| **드라이브 사운드트랙** · 음악을 직접 만드는 운전자·동승자 | 같은 20초 구간을 부드러운 출발과 반복 정차 패턴으로 재생한다. 속도 변화와 멈춘 구간을 각각 리듬과 쉼표로 바꾸고 주차 후 두 곡을 들어본다. | 부산 공모의 ‘카-케스트라 비트파티’는 주변 차와 음악 축제를 즐기는 아이디어다. 여기서는 한 번의 주행 기록으로 만든 두 결과를 듣고 자기 구간을 구별할 수 있는지 시험한다. 우선 직접 만든 음원과 모의 신호를 쓰고 실제 속도·정차 신호 접근은 별도 검증한다. |

## 선택 기록

11월 4일에는 후보를 보는 기준을 넓혔다. 해결할 불편과 함께 **차에 탈 때 기대하는 경험**도 다룬다. 현대차 Pleos Connect 적용 차량은 블루링크 스탠다드 최초 가입 시 앱마켓이 포함된 프리미엄을 5년간 무료 제공하며 이후 별도 가입 요금은 월 7,700원이다. 이는 앱마켓 이용 요금이다. 우리 앱의 별도 판매 가격이나 구독 유지 효과는 미정이다.

각 후보에는 세 장면을 적는다. **첫 탑승에서 얻는 것**, **다음 탑승에서 다시 열 계기**, **차량 신호나 오디오가 있어 달라지는 점**이다. 예를 들어 드라이브 사운드트랙은 첫날 만든 곡을 듣는 데서 끝날 수 있다. 다음 드라이브가 끝났을 때 사용자가 먼저 두 주행의 곡을 비교하거나 한 곡을 보관하는지 관찰하면 재사용 가설을 시험할 수 있다. 두 번째 이동 기회를 주고 앱을 다시 열었는지와 그 이유를 기록한다. 소수의 재사용 사례로 유료 전환율을 추정하지 않는다.

최종 주제와 우선순위는 미정이다. 차량에서 읽을 수 있는 신호, 같은 문제를 해결하는 기존 제품, 당사자를 만날 수 있는지를 후보별로 확인한 뒤 결정한다.

## 원본과 출처

- [기아 PBV 배송 전·중·후 시나리오](https://www.kia.com/kr/pbv/solution/about-solution) · [Apple CarPlay와 Siri의 장소 알림 예시](https://support.apple.com/guide/iphone/use-siri-in-your-car-iph0aa8c80e6/26/ios/26) · [Geo2 Package Finder](https://geo2.com/route-planner) · [Memo Push 체크리스트](https://www.memopush.com/) · [Pocket Lists 위치 알림](https://pocketlists.com/help/26/location-based-reminders/)
- [현대차그룹·42dot, 10월 30일 Gleo 업데이트](https://www.hyundaimotorgroup.com/ko/news/42dot-gleo-ai-first-ota-in-car-ai-experience-upgrade) · [Pleos 앱마켓 발표](https://www.hyundaimotorgroup.com/ko/news/42dot-pleos-connect-app-market-new-apps)

- [GearLog · 장비 대여·반납](https://play.google.com/store/apps/details?id=org.gearlog.app) · [MixShuffle · 차 안에서 음원 블라인드 비교](https://mixshuffle.com/)

- [CDC Yellow Book · 멀미의 감각 충돌과 연령별 민감도](https://www.cdc.gov/yellow-book/hcp/travel-air-sea/motion-sickness.html) · [제네시스 GV80 부분변경 모델의 승차감 기술](https://www.hyundaimotorgroup.com/ko/news/CONT0000000000115083)
- [현대차그룹 · 2026 아이디어 페스티벌 최우수상 ‘멀미 제로’](https://www.hyundaimotorgroup.com/ko/story/2026-idea-festival) · [Apple · 차량 모션큐](https://support.apple.com/ko-kr/guide/iphone/iph55564cb22/27/ios/27)

- [현대차그룹 · Pleos 기본 내비와 AI 길 안내](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) · [현대차그룹 · 기존 맛집 로드의 추천 범위](https://www.hyundaimotorgroup.com/ko/story/CONT0000000000140719)

- [Pleos 공식 SDK 변경 기록 · NaviHelper v3.0.5 remainTime](https://document.pleos.ai/en/docs/pleos-only/release-note/sdk-release-note)
- [Autio · 위치 기반 차량 여행 오디오](https://autio.com/) · [Google 지도 · 장소 저장과 우회 시간](https://blog.google/products-and-platforms/products/maps/google-maps-best-features-tips/)

- [현대자동차 블루링크 가입 · Pleos Connect 적용 차량 요금제](https://www.hyundai.com/kr/ko/digital-customer-support/app/myhyundai/bluelink-registration) · 국내 현대차 기준, 2026년 11월 4일 확인.

- [사용자 제공 설명회 스크린샷 ZIP](../sources/현대차앱공모전사진.zip) · 파일명 끝 143544(기획 방향), 143556(공모 분야), 143131(심사 기준).
- [공식 1차 평가 항목](https://appdevchallenge.auto-hmg.io/guide/judging-awards) · [공식 개발 환경](https://appdevchallenge.auto-hmg.io/guide/development).
- [실제 검증한 Vehicle 샘플 범위](05-first-run.md). 세 시연은 팀의 기획 가설이고 실제 고객 후기나 SDK 검증 결과와 구분한다.

- 현대모비스 배포 [블루링크 위치 공유 설명서](https://www.mobis.com/multi-product/FileDown.do?fileId=C1MS1BDK00&ifId=2041) · [URL 조회 방식](https://www.mobis-as.com/multi-product/FileDown.do?fileId=C1MS1BDK04&ifId=3974).
- Pleos [NaviHelper 이벤트 목록](https://document.pleos.ai/api-reference/connect-sdk-pleos/NaviHelper/event-listeners).

- [현대차 V2L 방전 제한과 전력 초과 안내](https://ownersmanual.hyundai.com/full_webhelp/NH2/2026/ko_KR/id225KAJ00BHS.html) · [원격 스마트 주차 보조 작동](https://ownersmanual.hyundai.com/full_webhelp/LX3HEV/2026/ko_KR/idc25ea14ed1f.html) · [부산 공모 우수 아이디어](https://www.hyundaimotorgroup.com/ko/story/hyundai-motor-company-pleos-connect-app-idea-contest).
