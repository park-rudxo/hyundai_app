# NAVER API HUB 장소 검색 서버

## 지금 할 일

`secrets.local.json` 파일의 빈 따옴표 안에 API HUB의 Client ID와 Client Secret을 각각 붙여넣고 저장하세요. 이 파일은 Git에서 제외됩니다. 내용을 채팅에 보내지 마세요. 예시는 secrets.example.json입니다. 기존 Maps 키와 다릅니다.

## 로컬 실행

Python 3에서 저장소 루트 기준:

```powershell
python server/place_search.py
```

브라우저에서 `http://127.0.0.1:8080/places?q=수원월드컵경기장`을 열어 결과를 확인합니다. 설정이 없으면 503, 검색 서비스 호출 실패는 502를 반환합니다. 서버를 재실행하지 않아도 다음 요청에 설정 파일을 읽습니다. 환경변수 NAVER_API_HUB_CLIENT_ID / NAVER_API_HUB_CLIENT_SECRET이 파일보다 우선합니다. 검색어와 인증 키는 요청 로그에 기록하지 않습니다.

## 배포

현재 localhost 전용이며 인터넷에 배포하지 않았습니다. 실제 앱 연결에는 HTTPS 주소가 필요합니다. 배포 시 로컬 파일 대신 호스팅 서비스의 비공개 환경변수에 키를 넣고, HTTPS 프록시와 요청 속도/사용량 제한을 설정하세요. 로컬 키 파일을 이미지·공개 저장소에 포함하지 마세요. 앱에는 중계 서버 주소만 넣고 Secret은 넣지 않습니다.

공식 근거:
- https://api.ncloud-docs.com/docs/naver-api-hub-overview
- https://api.ncloud-docs.com/docs/naver-api-hub-search-local

새 호출 주소는 https://naverapihub.apigw.ntruss.com/search/v1/local 입니다. 최대 결과 5개와 네이버 장소명·주소·좌표를 반환합니다. 실제 인증 호출은 로컬 키 입력 후 확인해야 합니다.
