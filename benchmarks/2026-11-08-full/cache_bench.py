import json, os, random, statistics, time, urllib.request, uuid

TOKEN = os.environ['INTERNAL_TOKEN']
URL = 'http://botrunningsystem:8082/bot/compile/'
run_id = uuid.uuid4().hex
sources = {
    'cpp': '#include <iostream>\nint main(){std::cout << 0 << std::endl;}\n// ',
    'java': 'public class Main { public static void main(String[] args) { System.out.println(0); } }\n// ',
    'python': 'print(0)\n# ',
    'go': 'package main\nimport "fmt"\nfunc main(){fmt.Println(0)}\n// ',
}
measurements = {lang: {'miss': [], 'hit': []} for lang in sources}
order = list(sources) * 10
random.Random(20261108).shuffle(order)
print(json.dumps({'event': 'start', 'run_id': run_id, 'seed': 20261108, 'pairs_per_language': 10}), flush=True)
for i, lang in enumerate(order):
    code = sources[lang] + run_id + '-' + str(i) + '\n'
    for kind in ('miss', 'hit'):
        request = urllib.request.Request(URL, data=json.dumps({'bot_language': lang, 'bot_code': code}).encode(), headers={'Content-Type': 'application/json', 'X-Internal-Token': TOKEN})
        start = time.perf_counter_ns()
        with urllib.request.urlopen(request, timeout=90) as response:
            body = response.read().decode()
            status = response.status
        elapsed_ms = (time.perf_counter_ns() - start) / 1e6
        print(json.dumps({'event':'request', 'pair':i, 'language':lang, 'kind':kind, 'ms':round(elapsed_ms, 3), 'http_status':status, 'response':body}), flush=True)
        if body != 'success':
            raise RuntimeError('Compile request failed')
        measurements[lang][kind].append(elapsed_ms)
for lang, values in measurements.items():
    miss = statistics.median(values['miss'])
    hit = statistics.median(values['hit'])
    print(json.dumps({'event':'summary', 'language':lang, 'miss_median_ms':round(miss,3), 'hit_median_ms':round(hit,3), 'median_ratio':round(miss/hit,2)}), flush=True)
