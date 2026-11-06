# 현대자동차그룹 차량용 앱 개발 공모전

Pleos 차량용 앱 공모전을 준비하며 공식 기능, 개발 환경, 기획 자료를 정리하는 저장소입니다.

**개인 참가 · 챙겨가 첫 실행 버전 개발 · 1차 접수 2026.11.15**

최근 정리: 2026.11.06

## 지금 하는 일

**사진으로 등록한 짐을 방문지와 연결하고, 출발 준비와 도착 후 전달을 확인하는 ‘챙겨가’(가칭)를 기획합니다.** [기존 서비스 조사](docs/04-idea-selection.md)

- [공식 양식에 작성한 기획서 초안](deliverables/챙겨가_공모전_기획서_초안.docx) · [모바일에서 읽는 기획 내용](docs/15-proposal-draft.md)
- 개인정보·동의·서명은 비워 두었습니다. 작성 후 개인정보가 포함된 제출본은 개인 기기에 보관합니다.
- [앱 소스·빌드·실행 방법](android/README.md) · [개발 및 검증 기록](docs/16-development-log.md) · [개발용 APK](apk/README.md)
- 구현: 방문지별 사진 등록, 적재·전달·되돌리기, 기기 내 저장, 같은 네트워크에서 휴대폰→차량 일회 전송(0.2.0). 실제 차량용 P단·UX 제한 읽기 확인.
- 다음 작업: 실제 두 기기 전송 시험, 내비 자동 도착, 출발 전 예약 알림. 무선 전송은 AVD 내부에서 검증했으며 지속 동기화는 아닙니다. 방문지는 직접 선택합니다.
- 숏폼은 앱 완성 후 진행합니다. 현재 영상과 제출용 release APK는 없습니다.
- 차멀미는 출품 후보에서 제외했습니다. [이전 조사 기록](docs/14-motion-sickness.md)

## 조사 자료

| 찾는 내용 | 바로 보기 |
|---|---|
| **Pleos 앱마켓에 어떤 앱이 있나?** | [확인한 앱·서비스와 아직 모르는 목록](docs/12-pleos-app-market.md) |
| 현대차·Tesla·중국 업체 앱은 무엇을 하나? | [공식 기능 비교표와 앱스토어 링크](docs/11-official-app-feature-map.md) |
| Pleos에서 어떤 기능을 개발할 수 있나? | [SDK·API 제공 범위](docs/08-pleos-apis.md) |

기능 비교표는 공식 기능 페이지·설명서·개발사 앱스토어 설명을 우선합니다. 제공 중인 기능과 발표·계획, 국가·차종·버전 차이를 구분합니다.

## 공모전과 작업 안내

- [참가 조건·심사 기준·제출 일정](docs/01-contest.md)
- [Pleos 샘플 빌드·실행 기록](docs/05-first-run.md) — 샘플 실행과 운전석 앞문 상태 조회까지 확인
- [다른 기기에서 이어서 작업하기](docs/07-handoff.md)
- [상세 작업 일정·진행 기록](docs/13-work-log.md)

<details>
<summary>이전 조사와 후보 기록 보기</summary>

아래 자료는 당시 조사 기록입니다. 현재 선정한 주제나 우선순위를 뜻하지 않습니다.

- [현대차그룹 기업 조사](docs/02-companies.md)
- [초기 비교 기업·출품 후보](docs/03-benchmarks.md)
- [아이디어 후보 기록](docs/04-idea-selection.md)
- [이용 경험·수상 사례](docs/06-experiences-and-awards.md)
- [불편 사례 검증 기록](docs/09-problem-evidence.md)

</details>

[공식 공모전 가이드](https://appdevchallenge.auto-hmg.io/guide) · [설명회 원본 자료](sources/현대차앱공모전사진.zip)
