import datetime, json, subprocess
urls = {'accounts': 'https://byteduel.de5.net/api/ranklist/getlist/?page=1', 'matches': 'https://byteduel.de5.net/api/record/getlist/?page=1'}
results = {}
for name, url in urls.items():
    response = subprocess.run(['curl', '-fsS', '--max-time', '20', url], check=True, capture_output=True, text=True)
    results[name] = {'url': url, 'total': json.loads(response.stdout)['total']}
print(json.dumps({'checked_at_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'results': results}, indent=2))
