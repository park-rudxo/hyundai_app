# 디자인 기준 적용 · 0.9.1

Pretendard Regular/SemiBold를 앱 리소스에 포함하고 Compose의 모든 Typography 역할에 연결했다. 폰 지도 검색의 기존 View에도 적용했다. Medium 역할은 현재 포함한 두 굵기에 매핑되므로 별도 Medium 파일 도입은 후속 작업이다.

Material Symbols Rounded의 기본 벡터를 공식 저장소에서 내려받아 뒤로·추가·장소·편집·더보기에 연결했다. RTL 뒤로 아이콘 미러링을 유지했다. 장식 아이콘의 접근성 설명은 생략하여 텍스트가 중복 읽히지 않도록 했다.

제목 26/34, 항목 17/24, 본문 16/24, 버튼 16/22 공통 스타일. 준비 화면 카드 내부 16dp, 버튼 모서리 12dp, 주요 버튼 최소 56dp. 폰·차량이 같은 글꼴과 아이콘을 사용한다. 레거시 차량/도구 화면의 개별 글자 크기는 아직 모두 공통 역할로 전환되지 않았다.

사진 배치는 이번 변경에서 조정하지 않았다. 앱 기능과 데이터 형식은 유지한다. 공식 라이선스는 앱 assets/licenses에 포함했다.

출처: https://github.com/orioncactus/pretendard
출처: https://github.com/google/material-design-icons

検証: 폰·차량·시연 빌드와 폰 단위 테스트 성공. 일반 실행 화면에서 Pretendard와 뒤로/더보기 아이콘 표시를 캡처 확인했다. 계측 테스트는 System UI 응답 없음과 Compose 테스트의 CalledFromWrongThreadException으로 정상 완료하지 못했으므로 UI 테스트 통과를 주장하지 않는다.
