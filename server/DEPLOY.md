# 휴대폰 검색 연결을 위한 배포

현재 앱 연결 코드는 준비됐지만 인터넷 서버는 아직 배포하지 않았습니다. 로컬 검증 APK는 adb reverse 연결이 있어야 동작합니다. 이 APK를 그대로 휴대폰 배포용으로 제공하지 않습니다.

## Docker 호스팅 설정

Docker Web Service를 지원하는 호스팅에 이 저장소를 연결합니다. 서비스 생성·요금제 선택은 본인 계정에서 진행하고 요금을 확인하세요.

- 빌드 컨텍스트/Root Directory: server
- Dockerfile: Dockerfile
- 환경변수: NAVER_API_HUB_CLIENT_ID, NAVER_API_HUB_CLIENT_SECRET (콘솔의 비공개 환경변수에 입력)
- BIND_HOST: 0.0.0.0
- PORT: 호스팅이 지정하는 값 (미지정 시 8080)

Docker 이미지에는 place_search.py만 복사합니다. 로컬 키 파일은 업로드하지 않습니다. HTTPS 주소가 생성되면 /places?q=수원월드컵경기장 으로 검색 결과를 확인합니다. 외부 공개 전 프록시의 요청 속도 제한을 설정하세요.

## 앱 빌드

android/local.properties에 `PLACE_SEARCH_URL=https://발급된서버주소`를 입력하고 APK를 다시 빌드합니다. 끝에 /places는 붙이지 않습니다. 앱은 Secret을 포함하지 않고 중계 서버만 호출합니다.

## 로컬 검증

`python server/place_search.py` 실행 후 `adb reverse tcp:8080 tcp:8080`을 설정합니다. 개발용 PLACE_SEARCH_URL=http://127.0.0.1:8080 을 사용합니다. HTTP는 debug의 localhost에만 허용합니다. 이 연결은 PC가 꺼지거나 USB/ADB 연결이 끊기면 작동하지 않습니다.

## Render에서 만드는 경우

1. https://dashboard.render.com 에 가입/로그인하고 GitHub 저장소 접근을 연결합니다.
2. New → Web Service에서 hyundai_app 저장소를 선택합니다.
3. Root Directory는 server, Language는 Docker, Dockerfile Path는 Dockerfile로 지정합니다.
4. 위 두 인증 환경변수를 입력하고 서비스 요금제를 확인해 배포합니다.
5. 생성된 HTTPS 주소의 /health가 {"ok":true}인지 확인합니다. HTTPS 주소만 개발자에게 전달합니다. 키는 전달하지 않습니다.

공식 Docker 배포 안내: https://render.com/docs/docker
