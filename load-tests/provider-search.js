import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CATEGORIES = ['', 'Haircut', 'Color', 'Consultation', 'Massage', 'Facial'];

export default function () {
    const category = CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)];
    const path = category ? `/api/v1/providers/search?category=${category}` : '/api/v1/providers';
    
    const res = http.get(`${BASE_URL}${path}`);
    check(res, { 'search returned 200': (r) => r.status === 200 });
    
    sleep(0.5);
}
