import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    const phone = `+1555${Math.floor(10000000 + Math.random() * 90000000)}`;
    
    // Request OTP
    const otpRes = http.post(`${BASE_URL}/api/v1/auth/request-otp`, JSON.stringify({
        phoneNumber: phone,
        channel: 'sms',
    }), { headers: { 'Content-Type': 'application/json' } });
    
    check(otpRes, { 'OTP request accepted': (r) => r.status === 202 });
    
    sleep(1);
}
