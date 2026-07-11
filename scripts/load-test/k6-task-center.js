import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: 100, duration: '3m', thresholds: { http_req_failed: ['rate<0.01'] } };

export default function () {
  const baseUrl = __ENV.OAF_BASE_URL || 'http://localhost:8080/api';
  const response = http.get(`${baseUrl}/tasks?pageNo=1&pageSize=10`, {
    headers: { Authorization: `Bearer ${__ENV.OAF_TOKEN}` }
  });
  check(response, { 'task center success': r => r.status === 200 });
}
