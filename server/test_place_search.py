import unittest,threading,http.client,json,io
from unittest.mock import patch
import place_search as app
class SearchTest(unittest.TestCase):
 def request(self,path):
  server=app.ThreadingHTTPServer(('127.0.0.1',0),app.Handler)
  thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
  try:
   c=http.client.HTTPConnection('127.0.0.1',server.server_port,timeout=3);c.request('GET',path)
   r=c.getresponse();result=(r.status,r.read());c.close();return result
  finally:server.shutdown();server.server_close();thread.join()
 def test_missing_credentials(self):
  with patch.object(app,'load_config',return_value={}):self.assertEqual(self.request('/places?q=test')[0],503)
 def test_invalid_query(self):self.assertEqual(self.request('/places?q=')[0],400)
 def test_hub_request_and_coordinates(self):
  payload={'items':[{'title':'<b>경기장</b>','roadAddress':'수원시','mapx':'1270310000','mapy':'372860000'}]}
  with patch.object(app,'load_config',return_value={'NAVER_API_HUB_CLIENT_ID':'test-id','NAVER_API_HUB_CLIENT_SECRET':'test-secret'}),patch.object(app.urllib.request,'urlopen',return_value=io.BytesIO(json.dumps(payload).encode())) as upstream:
   status,body=self.request('/places?q=stadium');self.assertEqual(status,200)
   place=json.loads(body)['places'][0];self.assertEqual(place['name'],'경기장');self.assertAlmostEqual(place['longitude'],127.031)
   req=upstream.call_args.args[0];self.assertTrue(req.full_url.startswith('https://naverapihub.apigw.ntruss.com/search/v1/local?'))
   self.assertEqual(dict((k.lower(),v) for k,v in req.header_items())['x-ncp-apigw-api-key'],'test-secret')
 def test_decimal_and_invalid_coordinates(self):
  self.assertEqual(app.coordinate('127.031',180),127.031)
  with self.assertRaises(ValueError):app.coordinate('nan',90)
if __name__=='__main__':unittest.main()
