import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: { upload: { executor: 'constant-arrival-rate', rate: 5, timeUnit: '1s', duration: '5m', preAllocatedVUs: 20, maxVUs: 100 } },
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<5000'] }
};

export default function () {
  const body = `# 压测文档 ${__VU}-${__ITER}\n\nOpenAgentFlow 文档物理DAG压测内容。`.repeat(100);
  const response = http.post(`${__ENV.OAF_BASE_URL}/knowledge-bases/${__ENV.OAF_KB_ID}/documents`, {
    file: http.file(body, `load-${__VU}-${__ITER}.md`, 'text/markdown')
  }, { headers: { Authorization: `Bearer ${__ENV.OAF_TOKEN}`, 'X-Workspace-Id': __ENV.OAF_WORKSPACE_ID } });
  check(response, { 'document accepted': r => r.status === 200 });
  sleep(1);
}
