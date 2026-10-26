import json,time,statistics,urllib.request
ENV=['PATH=/usr/bin:/bin','HOME=/tmp','GOCACHE=/tmp/gocache','GOPATH=/tmp/go','CGO_ENABLED=0','GOTOOLCHAIN=local']
def req(path, data=None, method=None):
 r=urllib.request.Request('http://127.0.0.1:5050'+path, data=None if data is None else json.dumps(data).encode(), headers={'Content-Type':'application/json'},method=method)
 with urllib.request.urlopen(r, timeout=100) as x:return json.load(x) if method!='DELETE' else x.read()
def run(args, source=None, cached=None, cpu=30000, clock=60000, mem=512, stdin=''):
 c={'args':args,'env':ENV,'files':[{'content':stdin},{'name':'stdout','max':65536},{'name':'stderr','max':65536}],'cpuLimit':cpu*1000000,'clockLimit':clock*1000000,'memoryLimit':mem*1024*1024,'procLimit':128}
 if source:c['copyIn']={source[0]:{'content':source[1]}}
 if cached:c['copyOutCached']=[cached]
 start=time.perf_counter();result=req('/run',{'cmd':[c]})[0];return time.perf_counter()-start,result
def show(name,values):print(name,'n=',len(values),'median=',round(statistics.median(values)*1000,2),'ms','values_ms=',[round(x*1000,2) for x in values],flush=True)
# Go template is injected by the caller.
GO_SOURCE = "package main\n\nimport (\n\t\"bufio\"\n\t\"fmt\"\n\t\"os\"\n\t\"strconv\"\n\t\"strings\"\n)\n\n// Runs once per turn: reads the game state as one line from stdin and prints a direction to stdout: 0 up, 1 right, 2 down, 3 left\n//\n// Input: map#my_sx#my_sy#(my_steps)#opp_sx#opp_sy#(opp_steps)\n//   - map: the 15 x 14 board as a row-by-row string of 0s and 1s, where 1 is a wall\n//   - steps: every move made so far, e.g. (0121)\n\nconst ROWS, COLS = 15, 14\n\nvar dx = [4]int{-1, 0, 1, 0}\nvar dy = [4]int{0, 1, 0, -1}\n\n// Same rule as the server: a snake grows on each of its first 10 steps, then on every 3rd step\nfunc checkTailIncreasing(step int) bool {\n\treturn step <= 10 || step%3 == 1\n}\n\n// Rebuild a snake's body from its start cell and steps; the last cell is the head\nfunc getCells(sx, sy int, steps string) [][2]int {\n\tsteps = steps[1 : len(steps)-1] // strip the parentheses\n\tcells := [][2]int{{sx, sy}}\n\tx, y := sx, sy\n\tfor i := 0; i < len(steps); i++ {\n\t\td := int(steps[i] - '0')\n\t\tx, y = x+dx[d], y+dy[d]\n\t\tcells = append(cells, [2]int{x, y})\n\t\tif !checkTailIncreasing(i + 1) {\n\t\t\tcells = cells[1:]\n\t\t}\n\t}\n\treturn cells\n}\n\nfunc atoi(s string) int {\n\tn, _ := strconv.Atoi(s)\n\treturn n\n}\n\nfunc nextMove(input string) int {\n\tstrs := strings.Split(input, \"#\")\n\tvar g [ROWS][COLS]bool\n\tfor i := 0; i < ROWS; i++ {\n\t\tfor j := 0; j < COLS; j++ {\n\t\t\tg[i][j] = strs[0][i*COLS+j] == '1'\n\t\t}\n\t}\n\n\tme := getCells(atoi(strs[1]), atoi(strs[2]), strs[3])\n\tyou := getCells(atoi(strs[4]), atoi(strs[5]), strs[6])\n\tfor _, c := range me {\n\t\tg[c[0]][c[1]] = true\n\t}\n\tfor _, c := range you {\n\t\tg[c[0]][c[1]] = true\n\t}\n\n\t// Example strategy: take the first direction that hits neither a wall nor a snake. Write your own strategy here.\n\thead := me[len(me)-1]\n\tfor d := 0; d < 4; d++ {\n\t\tx, y := head[0]+dx[d], head[1]+dy[d]\n\t\tif x >= 0 && x < ROWS && y >= 0 && y < COLS && !g[x][y] {\n\t\t\treturn d\n\t\t}\n\t}\n\treturn 0\n}\n\nfunc main() {\n\tinput, _ := bufio.NewReader(os.Stdin).ReadString('\\n')\n\tfmt.Println(nextMove(strings.TrimSpace(input)))\n}\n"
go=[]
for mode in ('cold','prebuilt'):
 vals=[]
 for n in range(10):
  cmd=('/bin/sh','-c',('cp -r /opt/gocache /tmp/gocache && ' if mode=='prebuilt' else '')+'/usr/bin/go build -o main main.go')
  t,r=run(cmd,('main.go',GO_SOURCE),'main');print('go',mode,n,r['status'],flush=True)
  if r['status']!='Accepted':print(json.dumps(r),flush=True);break
  vals.append(t); req('/file/'+r['fileIds']['main'],method='DELETE')
 if vals:show('go '+mode,vals)
# End-to-end sandbox execution timings, compiled once, constant output bot.
languages={
 'cpp':('main.cpp','#include <iostream>\nint main(){std::cout << 0 << std::endl;}',['/usr/bin/g++','-O2','-std=c++17','-pipe','-o','main','main.cpp'],'main',['./main']),
 'java':('Main.java','public class Main { public static void main(String[] args) { System.out.println(0); } }',['/bin/sh','-c','/usr/bin/javac -J-Xmx256m -J-XX:+UseSerialGC -encoding UTF-8 Main.java && /usr/bin/jar -J-Djava.io.tmpdir=. cf main.jar *.class'],'main.jar',['/usr/bin/java','-Xmx256m','-XX:+UseSerialGC','-XX:TieredStopAtLevel=1','-cp','main.jar','Main']),
 'python':('main.py','print(0)',['/usr/bin/python3','-m','py_compile','main.py'],'main.py',['/usr/bin/python3','main.py']),
 'go':('main.go','package main\nimport "fmt"\nfunc main(){fmt.Println(0)}',['/bin/sh','-c','cp -r /opt/gocache /tmp/gocache && /usr/bin/go build -o main main.go'],'main',['./main'])}
for lang,(filename,src,compileargs,output,runargs) in languages.items():
 _,r=run(compileargs,(filename,src),output)
 if r['status']!='Accepted':print('compile failed',lang,r,flush=True);continue
 fid=r['fileIds'][output];vals=[]
 for i in range(10):
  c={'args':runargs,'env':ENV,'files':[{'content':'\n'},{'name':'stdout','max':65536},{'name':'stderr','max':65536}],'cpuLimit':2000000000,'clockLimit':3000000000,'memoryLimit':512*1024*1024,'procLimit':128,'copyIn':{output:{'fileId':fid}}}
  t=time.perf_counter();res=req('/run',{'cmd':[c]})[0];vals.append(time.perf_counter()-t)
  if res['status']!='Accepted' or res.get('files',{}).get('stdout','').strip()!='0':print('run failure',lang,res,flush=True)
 show('execution '+lang,vals);req('/file/'+fid,method='DELETE')
for label,source in [('syntax','def broken(:\n pass'),('infinite','while True: pass'),('memory','a=bytearray(700*1024*1024)')]:
 args=['/usr/bin/python3','-m','py_compile','main.py'] if label=='syntax' else ['/usr/bin/python3','main.py']
 t,r=run(args,('main.py',source),cpu=2000,clock=3000)
 print('check',label,'status',r['status'],'elapsed_ms',round(t*1000,2),'stderr',r.get('files',{}).get('stderr','')[:200],flush=True)
