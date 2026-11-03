# 기기를 바꿔 이어서 작업하기

## 지금 이어받을 일

**11/3 · [1차 심사 기준 분석](01-contest.md)을 먼저 읽는다.** 분야는 자유이며 차량특화성·창의성·기술 활용성·사업성은 모두 평가 대상이다.

**다음 기기에 남기는 메모** — 아래 네 줄을 수정한 뒤 커밋한다. 휴대폰에서는 [이 파일 편집](https://github.com/park-rudxo/hyundai_app/edit/main/docs/07-handoff.md)을 열 수 있다.

- 마지막으로 끝낸 일 · [SDK 6종 조사](08-pleos-apis.md)와 [현대·기아 이용 경험 정리](06-experiences-and-awards.md)
- 확인한 사실 · 1차 필수 평가 네 항목, 세부 배점 비공개, 개발 산출물은 선택 가산점
- 남은 질문 · 각 항목을 어떤 근거로 보여줄 수 있을까?
- 다음 한 가지 · [최근 불편 검증](09-problem-evidence.md)의 아이오닉 5 사례를 참고해 주차 상태에서 공조 변경 전후를 순정 화면만으로 알아볼 수 있는지 관찰한다. 다른 차종이면 별도 사례로 기록한다.

## 싸피 PC → 노트북 → 휴대폰 → PC

1. **처음 노트북에서** 터미널로 `git clone https://github.com/park-rudxo/hyundai_app.git`을 실행하고 `cd hyundai_app`으로 들어간다. 문서는 바로 읽을 수 있다. 빌드·차량 시연도 할 노트북이라면 [실행 안내](05-first-run.md)에 따라 OS에 맞는 JDK·Android SDK·Pleos 이미지를 별도 설치한다. Git은 설치 프로그램, 로컬 실습 샘플 폴더, 가상 차량 상태를 복제하지 않는다.
2. **두 PC 공통**으로 작업 시작 시 `git status -sb`로 미완료 변경을 확인하고 깨끗하면 `git pull --ff-only`한다. 마치면 수정 파일만 `git add <파일>` → `git commit -m "작업 내용"` → `git push`하고 GitHub에서 최신 커밋을 확인한다. 로컬 수정이나 push 거절이 있으면 덮어쓰지 말고 먼저 작업 브랜치에 커밋해 보존한다.
3. **휴대폰**은 저장소 쓰기 권한이 있는 GitHub 계정으로 브라우저 로그인 후 위 편집 링크에서 메모·출처·발표 대본을 수정한다. **Commit changes**까지 눌러야 PC에서 보인다. 직접 커밋이 제한되면 새 브랜치로 커밋하고 PR을 열어 병합한다. GitHub Mobile 앱에서는 PR이나 이슈를 읽고 의견을 남길 수 있다. 통신이 어려우면 휴대폰 메모장에 초안을 저장하고 접속 가능할 때 반영한다.
4. **돌아온 PC**에서 다시 pull한 뒤 모바일에서 적은 마지막 한 줄부터 시작한다. 새 채팅으로 바뀌면 저장소와 이 문서 링크를 붙여 준다. 제출용 서명·연락처와 서명 키는 개인 기기에 보관한다.

## 11월 9일까지 해볼 연결 시험

- 노트북에서 저장소를 받고 이 문서의 다음 작업을 읽는다.
- 휴대폰에서 메모 한 줄을 수정해 커밋한다.
- PC에서 pull하고 같은 한 줄과 커밋을 확인한다. 세 기기 왕복이 확인되면 여기에 날짜를 적는다. **현재는 사용자 기기에서 확인 전이다.**

## 원본과 출처

- [GitHub 공식 파일 편집 안내](https://docs.github.com/en/repositories/working-with-files/managing-files/editing-files)
- [GitHub Mobile에서 가능한 작업](https://docs.github.com/en/get-started/using-github/github-mobile)
- [프로젝트 저장소](https://github.com/park-rudxo/hyundai_app)
