# 검색 서버 배포와 앱 연결

현재 앱은 Render에 배포한 `https://hyundai-app-xc7k.onrender.com`을 사용합니다. 아래 절차는 새 서버 배포 또는 재배포를 위한 안내입니다.

## Render Docker 설정

| 항목 | 설정 |
|---|---|
| 서비스 | Docker Web Service |
| 저장소 | park-rudxo/hyundai_app |
| Root Directory | `server` |
| Dockerfile Path | `Dockerfile` |
| API 인증 환경변수 | `NAVER_API_HUB_CLIENT_ID`, `NAVER_API_HUB_CLIENT_SECRET` |
| 수신 주소 | 이미지 기본값 `BIND_HOST=0.0.0.0` |
| 포트 | 플랫폼의 `PORT` 값, 미지정 시 8080 |

1. Render에서 GitHub 저장소를 연결하고 위 설정으로 서비스를 만듭니다.
2. API 인증정보는 Render의 비공개 환경변수에 등록합니다.
3. 배포 후 `https://서비스주소/health`를 확인합니다.
4. `/places?q=수원월드컵경기장`에서 실제 검색 결과를 확인합니다.
5. 요금제·절전 정책·요청량을 운영 환경에 맞게 관리합니다.

Docker 이미지에는 `place_search.py`만 들어갑니다. 로컬 인증 파일은 복사하지 않습니다. 서버는 Python 표준 라이브러리만 사용합니다.

## 앱에 연결

`android/local.properties`에 기본 주소를 설정하고 휴대폰 APK를 다시 빌드합니다.

```properties
PLACE_SEARCH_URL=https://hyundai-app-xc7k.onrender.com
```

주소 끝에 `/places`는 붙이지 않습니다. 앱에는 API Secret을 넣지 않습니다. 지도용 `NAVER_MAP_CLIENT_ID`는 별도로 설정합니다. [전체 앱 설정](../android/README.md)

## 로컬 에뮬레이터 연결

저장소 루트에서:

```powershell
python server/place_search.py
adb reverse tcp:8080 tcp:8080
```

로컬 시험 APK의 `PLACE_SEARCH_URL`은 `http://127.0.0.1:8080`으로 설정합니다. HTTP는 debug의 localhost에만 허용됩니다. 이 방식은 개발 PC와 ADB 연결이 있을 때 사용하며, 휴대폰 배포용 APK에는 HTTPS 서버 주소를 사용합니다.

## 갱신 후 확인

서버 코드 변경은 호스팅에 재배포합니다. 인증정보 변경은 비공개 환경변수에서 처리합니다. 앱의 서버 주소를 바꾸면 APK도 다시 빌드합니다. `/health` 응답과 실제 `/places` 응답을 각각 확인하세요.

[Render 공식 Docker 안내](https://render.com/docs/docker) · [서버 API](README.md)
