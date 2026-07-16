import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.OAF_VUS || 100),
  duration: __ENV.OAF_DURATION || '5m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<3000', 'p(99)<5000'],
  },
};

export default function () {
  const headers = { Authorization: `Bearer ${__ENV.OAF_TOKEN || ''}` };
  const response = http.get(`${__ENV.OAF_BASE_URL}/runs/cursor?pageSize=10`, { headers });
  check(response, { '运行游标查询成功': (item) => item.status === 200 });
  sleep(0.1);
}
