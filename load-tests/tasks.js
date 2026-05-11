import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
    stages: [
        { duration: '30s', target: 10 },   // ramp-up
        { duration: '1m', target: 50 },    // hold
        { duration: '30s', target: 100 },  // spike
        { duration: '30s', target: 0 },    // ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        errors: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function getToken() {
    const payload = JSON.stringify({
        email: 'admin@projem.kz',
        password: 'Admin1234!',
    });

    const res = http.post(`${BASE_URL}/api/v1/auth/login`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    if (res.status !== 200) return null;
    return JSON.parse(res.body).accessToken;
}

export function setup() {
    const token = getToken();
    if (!token) throw new Error('Failed to authenticate for load test');
    return { token };
}

export default function (data) {
    const headers = {
        Authorization: `Bearer ${data.token}`,
        'Content-Type': 'application/json',
    };

    // Get all tasks
    const list = http.get(`${BASE_URL}/api/v1/tasks`, { headers });
    check(list, {
        'tasks list status 200': (r) => r.status === 200,
        'tasks list response time < 200ms': (r) => r.timings.duration < 200,
    }) || errorRate.add(1);

    // Create a task
    const createPayload = JSON.stringify({
        title: `Load test task ${Date.now()}`,
        priority: 'MEDIUM',
    });

    const create = http.post(`${BASE_URL}/api/v1/tasks`, createPayload, { headers });
    check(create, {
        'task created': (r) => r.status === 201,
    }) || errorRate.add(1);

    sleep(1);
}
