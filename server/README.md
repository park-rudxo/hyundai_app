# 네이버 장소 검색 중계 서버

휴대폰 앱의 장소명 검색 요청을 NAVER API HUB 지역 검색 API로 전달하는 Python 서버입니다. 인증정보는 서버에 두고 앱에는 HTTPS 주소만 설정합니다.

**배포 주소:** https://hyundai-app-xc7k.onrender.com

## API

| 요청 | 응답 |
|---|---|
| `GET /health` | 프로세스 상태 `{"ok":true}` |
| `GET /places?q=수원월드컵경기장` | `places` 목록. 최대 5개의 장소명·주소·위도·경도 |

`/health`는 서버 응답을 확인합니다. 검색 인증과 실제 결과는 `/places`로 확인합니다. 잘못된 요청은 400, 인증 설정이 없으면 503, 검색 서비스 호출 실패는 502를 반환합니다. 서버는 요청 로그에 검색어와 인증 키를 남기지 않습니다.

## 인증 설정

배포 서비스의 비공개 환경변수에 다음 두 값을 등록합니다.

- `NAVER_API_HUB_CLIENT_ID`
- `NAVER_API_HUB_CLIENT_SECRET`

휴대폰 지도용 Maps Client ID와 장소 검색용 API HUB 인증정보는 서로 다릅니다. Client Secret은 APK에 포함하지 않습니다.

로컬 개발에서는 `secrets.example.json`을 `secrets.local.json`으로 복사해 입력할 수도 있습니다. 실제 값이 들어간 파일은 Git에서 제외되며 환경변수가 로컬 파일보다 우선합니다.

## 로컬 실행

저장소 루트에서 Python 3으로 실행합니다. 별도 Python 패키지 설치는 필요하지 않습니다.

```powershell
python server/place_search.py
```

기본 주소는 `http://127.0.0.1:8080`입니다. 브라우저에서 `/health`와 `/places?q=수원월드컵경기장`을 확인합니다. 앱을 로컬 서버에 연결하는 방법은 [배포·앱 설정](DEPLOY.md)을 참고하세요.

## 배포

[Dockerfile](Dockerfile)은 Python 3.12 기반으로 서버 코드만 복사하고 일반 사용자 권한으로 실행합니다. Render에서는 Root Directory를 `server`, 환경을 Docker로 설정합니다. 키는 Render 환경변수에 등록하고 HTTPS 기본 주소를 앱의 `PLACE_SEARCH_URL`에 넣습니다.

호스팅의 절전 상태와 네이버 API 응답에 따라 첫 검색은 시간이 걸릴 수 있습니다. 앱은 시작할 때 서버를 준비하고, 검색창 입력 후 결과를 표시합니다. 공개 운영 시 요청량과 호스팅·API 사용량을 관리합니다.

[배포·재배포 순서](DEPLOY.md) · [앱 빌드 설정](../android/README.md)

API 호출 주소: `https://naverapihub.apigw.ntruss.com/search/v1/local`

공식 자료: [API HUB 개요](https://api.ncloud-docs.com/docs/naver-api-hub-overview) · [지역 검색](https://api.ncloud-docs.com/docs/naver-api-hub-search-local)
