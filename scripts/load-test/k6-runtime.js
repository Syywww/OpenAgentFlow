import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    runtime: { executor: 'ramping-vus', startVUs: 0, stages: [
      { duration: '1m', target: 50 }, { duration: '3m', target: 200 }, { duration: '1m', target: 0 }
    ] }
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<3000', 'p(99)<8000']
  }
};

export default function () {
  const baseUrl = __ENV.OAF_BASE_URL || 'http://localhost:8080/api';
  const token = __ENV.OAF_TOKEN;
  const agentId = __ENV.OAF_AGENT_ID;
  const response = http.post(`${baseUrl}/chat/completions`, JSON.stringify({
    agentId, messages: [{ role: 'user', content: '请简要介绍产品能力' }]
  }), { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } });
  check(response, { 'runtime success': r => r.status === 200 });
  sleep(1);
}
