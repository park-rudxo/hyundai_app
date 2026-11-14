"""Naver local search proxy. Keep credentials on the server only."""
import os,json,html,re,urllib.request,urllib.parse
from pathlib import Path
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
def load_config():
 path=Path(__file__).with_name('secrets.local.json')
 data=json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else {}
 return {k:os.environ.get(k) or data.get(k,'') for k in ('NAVER_API_HUB_CLIENT_ID','NAVER_API_HUB_CLIENT_SECRET')}

def coordinate(value,limit):
 n=float(value)
 # Accept decimal WGS84 and the legacy 1e7-scaled representation.
 if abs(n)>limit:n/=10000000
 if not -limit<=n<=limit:raise ValueError('Invalid coordinate')
 return n

class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  u=urllib.parse.urlparse(self.path)
  if u.path=='/health':
   self.send_response(200);self.send_header('Content-Type','application/json');self.end_headers();self.wfile.write(b'{"ok":true}');return
  q=urllib.parse.parse_qs(u.query).get('q',[''])[0].strip()
  if u.path!='/places' or not 1<=len(q)<=100:
   self.send_error(400);return
  try:config=load_config()
  except (ValueError,OSError):self.send_error(503);return
  key=config.get('NAVER_API_HUB_CLIENT_ID');secret=config.get('NAVER_API_HUB_CLIENT_SECRET')
  if not key or not secret:self.send_error(503);return
  try:
   req=urllib.request.Request('https://naverapihub.apigw.ntruss.com/search/v1/local?'+urllib.parse.urlencode({'query':q,'display':5,'format':'json'}),headers={'X-NCP-APIGW-API-KEY-ID':key,'X-NCP-APIGW-API-KEY':secret})
   with urllib.request.urlopen(req,timeout=8) as r:data=json.load(r)
   places=[{'name':html.unescape(re.sub('<[^>]*>','',x['title'])),'address':x.get('roadAddress') or x.get('address',''),'latitude':coordinate(x['mapy'],90),'longitude':coordinate(x['mapx'],180)} for x in data.get('items',[])]
   body=json.dumps({'places':places},ensure_ascii=False).encode()
   self.send_response(200);self.send_header('Content-Type','application/json; charset=utf-8');self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(body)
  except Exception:self.send_error(502,'Place search unavailable')
 def log_message(self,*args):pass
if __name__=='__main__':ThreadingHTTPServer((os.environ.get('BIND_HOST','127.0.0.1'),int(os.environ.get('PORT','8080'))),Handler).serve_forever()
