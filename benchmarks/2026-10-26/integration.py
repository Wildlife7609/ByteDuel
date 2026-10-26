import urllib.request,urllib.error,json
base='http://web:80'
def req(path,data=None,token=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 r=urllib.request.Request(base+path,None if data is None else json.dumps(data).encode(),headers)
 try:
  with urllib.request.urlopen(r,timeout=20) as x:return x.status,x.read().decode()
 except urllib.error.HTTPError as e:return e.code,e.read().decode()
def call(name,path,data=None,token=None):
 status,body=req(path,data,token); print(name,'status=',status,'body=',body[:200] if name!='login' else '(redacted)',flush=True);return status,body
call('homepage','/')
call('internal endpoint block','/api/pk/start/game/',{})
u={'username':'benchtest2','password':'test-pass-123','confirmedPassword':'test-pass-123'}
status,body=call('register','/api/user/account/register/',u)
status,body=call('login','/api/user/account/token/',{'username':u['username'],'password':u['password']})
token=json.loads(body).get('token')
if not token:raise RuntimeError('No login token')
bot={'title':'Valid','description':'test','language':'python','content':'print(0)'}
call('valid bot','/api/user/bot/add/',bot,token)
bot.update(title='Invalid',content='def broken(:\n pass')
call('invalid bot','/api/user/bot/add/',bot,token)
for i in range(6):
 status,body=req('/api/user/account/token/',{'username':'benchtest2','password':'wrong-password'})
 print('wrong password attempt',i+1,'status',status,'body',body[:160],flush=True)
