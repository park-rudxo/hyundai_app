# 동승자 차멀미와 Pleos 앱으로 검증할 수 있는 것

2026년 11월 4일. 개인 참가자가 구현할 첫 장면으로 **주행 패턴을 유형별로 감지해 운전자에게 짧게 음성으로 알려주는 기능**을 검토한다. 지도상의 예상 움직임, 실제 차량 움직임, 동승자가 느낀 증상을 각각 기록한다. 셋 사이의 관계가 연구 대상이다.

## 사람을 대상으로 한 연구

| 원 연구 | 무엇을 비교했나 | 결과와 앱에 적용할 때의 한계 |
|---|---|---|
| Xie 등, 2026 [1] | 운전자·동승자 16쌍이 약 30분 도심 경로에서 가속도 피드백·시각 경고를 켠 조건과 끈 조건을 경험 | 안내를 받을 때 가벼운 증상 이상(MISC ≥ 2)을 경험할 가능성이 낮았다. 중등도·중증과 전체 MSDV에는 유의한 차이가 없었다. **주행 중 피드백** 연구이므로 시각 경고를 청각 피드백으로 바꿨을 때의 효과는 따로 시험해야 한다. |
| Souman 등, 2026 [2] | 세 통제 실험의 총 107명에게 전후·좌우·상하 방향으로 크기와 주기를 달리한 흔들림을 가함 | 전반적으로 0.2 Hz 부근(약 5초 주기)에서 증상이 컸고, 가속도가 클수록 증상이 심해졌다. 다른 주파수에서도 증상이 있었다. 5초마다 정지하면 멀미한다는 도로 임계값으로 옮길 수 없다. |
| Kehl 등, 2026 [3] | 40명이 영상을 보며 일반 도로를 두 번씩 30분 주행. 한 조건에서 좌·우회전 1초 전 진동 벨트로 방향을 알림 | 예고 조건에서 주관적 멀미가 줄었다. 몸에 닿는 방향성 진동과 정확한 예고 시점이 있는 실험이다. 중앙 화면이나 음성으로 바꿨을 때 같은 효과가 나는지는 미확인이다. |
| Jain 등, 2023 [4] | 굽은 경로에서 차선 안 궤적과 속도를 함께 최적화. 이동식 시뮬레이터에서 16명 평가 | 연구 조건에서 계산한 흔들림 노출량(MSDV)은 최대 52%, 참가자 증상 평점은 평균 65% 감소했다. 도로 두 개 중 직선 도로를 고른 실험이 아니므로 지도 추천의 효과 수치로 인용할 수 없다. |
| 빛·음악·향·환기를 결합한 동승 실험 [5] | 조수석에서 게임을 하는 53명을 대상으로 복합 자극을 평가 | 전체 주관적 평가와 생리 측정에서 유의한 개선이 없었다. 바람이나 조명 하나를 켜면 효과가 난다고 볼 근거도 없다. |

CDC는 동승자가 바깥의 먼 기준점을 볼 수 있게 하거나 독서를 피하는 방법에 근거가 있다고 정리한다. 이를 앱에 적용한다면 화면을 더 오래 봐야 하는 실시간 계기판의 득실도 검토해야 한다. [6]

## 구현 경계

| 제안 | 공개 인터페이스에서 확인한 것 | 시연 전 확인할 것 |
|---|---|---|
| 가다서다 한 구간 돌아보기 | Pleos FAQ는 Android Automotive의 CarPropertyManager로 PERF_VEHICLE_SPEED 읽기·구독을 안내한다. 값은 m/s이고 Car.PERMISSION_SPEED 권한이 필요하다. [7, 8] | AVD에서 권한 승인, 신호 시각, 수신 간격, 정지·출발 시 값 변화를 확인한다. 기존 프로젝트가 실측한 것은 운전석 문 상태뿐이다. |
| 굽은 길 미리 알기 | NaviHelper는 경로·현재 위치·회전 안내(TBT) 관련 콜백을 공개한다. 경로 재탐색도 지원한다. [9] | 안내할 교차로의 회전과 도로 자체의 굽이는 다르다. 전체 경로 형상·대안별 형상·예상 속도 확보 여부를 확인한다. 미확보 시 사전 입력한 한 구간을 예시 지도로 명시한다. |
| 공조·조명·차량 움직임 제어 | 공개 신청 안내에서 Vehicle State와 NaviHelper는 바로 사용 가능, Vehicle Control은 준비 중이다. 서버 Vehicle Data API에는 종가속도·요 각속도 필드가 있지만 별도 인증 체계이고 공개 지역 안내는 유럽이다. [10, 11] | 국내 일반 출품 앱이 공조·조명·시트 진동·제동·서스펜션을 직접 제어할 수 있음을 확인하지 못했다. 이번 구현은 읽기와 앱 자체 출력으로 계획한다. |

## 주행 중 운전자에게 들려줄 피드백

| 유형 | 속도 기록에서 찾을 장면 | 재생할 한 문장 |
|---|---|---|
| 재출발 때 빠르게 속도를 올리는 패턴 | 정지 부근에서 출발한 직후 짧은 시간 안에 속도가 크게 오르는 일이 여러 출발에서 반복된다. | “다음 출발은 조금 더 부드럽게 해 보세요.” |
| 주행 중 속도를 올렸다 줄이기를 반복하는 패턴 | 차가 계속 움직이는 동안 의미 있는 가속과 감속이 짧은 간격으로 교대한다. 출발 구간은 첫 유형에만 배정한다. | “가능할 때 속도 변화를 조금 줄여 보세요.” |

현재 주행의 짧은 속도 기록만 사용한다. 운전자의 의도, 앞차 상황, 동승자의 증상은 차속으로 판정할 수 없다. 단발 급제동·일반 정차·후진·데이터 단절은 안내 대상에서 제외한다. 패턴이 끝나고 차속이 안정됐을 때 짧은 녹음 음성을 한 번 재생한다. 같은 상황에는 재생 대기 시간을 두고, 다른 안내가 오디오를 사용 중이면 건너뛴다. 동승자 멀미 연구 [1]의 피드백은 시각 방식이어서 이 음성 코칭의 효과는 별도로 확인해야 한다.

## 첫 시연과 판정 기준

1. Pleos AVD에서 속도 권한과 실제 수신 간격을 확인한다 [7, 8]. 불규칙한 시간 간격과 측정 잡음을 고려해 속도 변화폭·지속 시간·반복 횟수를 조정하고 시연용 설정값을 기록한다.
2. 서로 다른 경로의 샘플에서 두 패턴이 각각 해당 음성을 한 번만 재생하는지 확인한다. 매끄러운 출발, 단발 급제동, 신호 단절 뒤 재수신에는 안내가 나가지 않아야 한다. 시뮬레이션 데이터와 실제 구독 결과를 표시한다.
3. 주차 상태에서 시연을 들려준 뒤 운전자가 다음에 어떤 행동을 바꿀지 설명할 수 있는지, 잦거나 부적절한 안내가 있었는지 기록한다. 멀미 개선 효과는 별도 검증 과제다.

## 원본과 출처

1. [Xie 등 · 운전자 피드백 실차 논문](https://doi.org/10.1080/10447318.2026.2628141) · [저자 공개 원고 PDF](https://personal.hkust-gz.edu.cn/hedengbo/assets/publicationPDFs/Xie_IJHCI_2026a.pdf)
2. [Souman 등 · 가속도 방향·크기·주기 실험](https://pubmed.ncbi.nlm.nih.gov/42240048/)
3. [Kehl 등 · 실제 도로에서 회전 전 진동 예고](https://pubmed.ncbi.nlm.nih.gov/41849854/)
4. [Jain 등 · 궤적·속도 최적화 시뮬레이터 연구](https://doi.org/10.1109/TITS.2023.3281724)
5. [동승자 대상 복합 자극 실험](https://pubmed.ncbi.nlm.nih.gov/39486345/)
6. [CDC Yellow Book · 행동 개입과 개인차](https://www.cdc.gov/yellow-book/hcp/travel-air-sea/motion-sickness.html)
7. [Pleos 공식 FAQ · 계기판 속도 조회](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq)
8. [Android Developers · PERF_VEHICLE_SPEED](https://developer.android.com/reference/android/car/VehiclePropertyIds#PERF_VEHICLE_SPEED)
9. [Pleos NaviHelper 이벤트](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/event-listeners) · [경로 옵션](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/routes)
10. [Pleos SDK 신청 상태](https://document.pleos.ai/en/docs/pleos-only/create-my-project)
11. [서버 Vehicle Data API 지역·인증](https://document.pleos.ai/en/api-reference/vehicle-data-api/intro) · [운행 데이터](https://document.pleos.ai/api-reference/vehicle-data-api/vehicle-data-api/common/get-vehicle-operation-data)
12. [TomTom · 굽은 구간을 피해 경로 고르는 예제](https://docs.tomtom.com/maps-sdk-js/guides/services/routing/curves)
