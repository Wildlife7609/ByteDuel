import asyncio, json, time, urllib.request, urllib.error
import websockets
BASE = 'http://localhost:18081'
SOURCES = {
    'cpp': '#include <iostream>\nint main(){std::cout<<1<<std::endl;}\n',
    'java': 'public class Main { public static void main(String[] args) { System.out.println(3); } }\n',
    'python': 'print(1)\n',
    'go': 'package main\nimport "fmt"\nfunc main(){fmt.Println(3)}\n',
}
def log(event, **data):
    print(json.dumps({'event':event, **data}), flush=True)
def api(path, data=None, token=None):
    headers = {'Content-Type':'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE+path, None if data is None else json.dumps(data).encode(), headers)
    try:
        with urllib.request.urlopen(req, timeout=60) as r: return r.status, r.read().decode()
    except urllib.error.HTTPError as e: return e.code, e.read().decode()
def check(condition, message):
    if not condition: raise AssertionError(message)
def create_account(username):
    password = 'isolated-test-pass-123'
    status, body = api('/api/user/account/register/', {'username':username,'password':password,'confirmedPassword':password})
    log('register', username=username, status=status, response=body)
    check(status==200 and json.loads(body).get('error_message')=='success', 'registration failed')
    status, body = api('/api/user/account/token/', {'username':username,'password':password})
    token = json.loads(body).get('token')
    log('login', username=username, status=status, token_present=bool(token))
    check(status==200 and token, 'login failed')
    return token
def create_bot(token, lang):
    start=time.perf_counter()
    status, body = api('/api/user/bot/add/', {'title':lang,'description':'isolated integration','language':lang,'content':SOURCES[lang]}, token)
    log('add_bot', language=lang, status=status, response=body, ms=round((time.perf_counter()-start)*1000,3))
    check(status==200 and json.loads(body).get('error_message')=='success', 'bot creation failed')
    status, body = api('/api/user/bot/getlist/', token=token)
    bots = json.loads(body)
    return next(b['id'] for b in bots if b['language']==lang)
async def match(token_a, token_b, bot_a, bot_b, languages):
    async with websockets.connect('ws://localhost:18081/websocket/'+token_a, open_timeout=15) as a, websockets.connect('ws://localhost:18081/websocket/'+token_b, open_timeout=15) as b:
        await asyncio.gather(a.send(json.dumps({'event':'start-matching','bot_id':bot_a})), b.send(json.dumps({'event':'start-matching','bot_id':bot_b})))
        async def listen(ws, player):
            events=[]
            while True:
                msg=json.loads(await asyncio.wait_for(ws.recv(),timeout=35))
                event=msg.get('event')
                events.append(event)
                details={'player':player,'type':event,'languages':languages}
                if event=='match-success': details['map_rows']=len(msg['game']['map']); details['map_columns']=len(msg['game']['map'][0])
                if event=='move': details['a_step']=msg['a_step']; details['b_step']=msg['b_step']
                if event=='result': details['loser']=msg['loser']
                log('websocket', **details)
                if event=='result':
                    check('match-success' in events and 'move' in events, 'missing match or move event')
                    return events
        await asyncio.gather(listen(a,'A'),listen(b,'B'))
def main():
    status, body=api('/')
    log('homepage',status=status,has_html='<!DOCTYPE html>' in body)
    check(status==200, 'homepage failed')
    status,body=api('/api/pk/start/game/', {})
    log('blocked_internal',status=status)
    check(status in (403,404), 'internal endpoint exposed')
    a=create_account('bench_alpha'); b=create_account('bench_beta')
    bots_a={lang:create_bot(a,lang) for lang in ('cpp','python')}
    bots_b={lang:create_bot(b,lang) for lang in ('java','go')}
    status,body=api('/api/user/bot/add/',{'title':'invalid','description':'invalid','language':'python','content':'def broken(:\n pass'},a)
    log('reject_invalid_python',status=status,response=body)
    check(status==200 and json.loads(body).get('error_message')!='success', 'invalid bot accepted')
    status,body=api('/api/record/getlist/?page=1'); before=json.loads(body)['total']
    for la,lb in [('cpp','java'),('python','go')]: asyncio.run(match(a,b,bots_a[la],bots_b[lb],[la,lb]))
    status,body=api('/api/record/getlist/?page=1'); after=json.loads(body)['total']
    log('saved_matches',before=before,after=after)
    check(after-before==2,'two match records not saved')
    log('complete', result='pass')
if __name__=='__main__': main()
