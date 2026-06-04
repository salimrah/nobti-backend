import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<3000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    // Check queue status for a random-ish provider
    const providerId = '00000000-0000-0000-0000-000000000001';
    
    const statusRes = http.get(`${BASE_URL}/api/v1/queues/providers/${providerId}/status`);
    check(statusRes, { 'queue status returned 200': (r) => r.status === 200 });
    
    const membersRes = http.get(`${BASE_URL}/api/v1/queues/providers/${providerId}/members`);
    check(membersRes, { 'queue members returned 200': (r) => r.status === 200 });
    
    sleep(2);
}
