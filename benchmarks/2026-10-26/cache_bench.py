import json,statistics,time,urllib.request,urllib.error
base='http://botrunningsystem:8082/bot/compile/'
cold=[];warm=[]
for i in range(10):
 code='package main\nimport "fmt"\nfunc main(){fmt.Println(0)}\n// benchmark case %d\n'%i
 for label,values in [('miss',cold),('hit',warm)]:
  req=urllib.request.Request(base,data=json.dumps({'bot_language':'go','bot_code':code}).encode(),headers={'Content-Type':'application/json','X-Internal-Token':TOKEN})
  t=time.perf_counter()
  with urllib.request.urlopen(req,timeout=90) as resp: body=resp.read().decode()
  delta=time.perf_counter()-t
  print(label,i,round(delta*1000,2),'ms',body[:200],flush=True)
  if body!='success':raise RuntimeError('Compilation failed')
  values.append(delta)
for label,v in [('miss',cold),('hit',warm)]:print(label,'median_ms',round(statistics.median(v)*1000,2),'all_ms',[round(x*1000,2) for x in v],flush=True)
