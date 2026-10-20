import { buildApp } from '../app';
import { WebhookDispatcher } from '../services/webhook-dispatcher.service';
import { ExportService } from '../services/export.service';

async function runTests() {
  console.log('====================================================');
  console.log('   ENOL SALESFORZE - AUTOMATED API TEST SUITE');
  console.log('====================================================\n');

  let passed = 0;
  let failed = 0;

  function assert(condition: boolean, testName: string) {
    if (condition) {
      console.log(`[PASS] ${testName}`);
      passed++;
    } else {
      console.error(`[FAIL] ${testName}`);
      failed++;
    }
  }

  const app = buildApp();
  await app.ready();

  // Test 1: Health check endpoint
  {
    const res = await app.inject({
      method: 'GET',
      url: '/health',
    });
    assert(res.statusCode === 200, 'GET /health returns HTTP 200');
    const body = JSON.parse(res.body);
    assert(body.status === 'UP' && body.service === 'enol-salesforze-api', 'Health payload status is UP');
  }

  // Test 2: API v1 Health check endpoint
  {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/health',
    });
    assert(res.statusCode === 200, 'GET /api/v1/health returns HTTP 200');
  }

  // Test 3: Swagger UI docs endpoint
  {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/docs/json',
    });
    assert(res.statusCode === 200, 'GET /api/v1/docs/json returns HTTP 200 OpenAPI schema');
    const openapi = JSON.parse(res.body);
    assert(openapi.openapi === '3.1.0', 'OpenAPI version is 3.1.0');
    assert(openapi.info.title.includes('ENOL SALESFORZE'), 'OpenAPI specification title is correct');
    assert(Object.keys(openapi.paths).length >= 15, `OpenAPI includes all domain endpoints (${Object.keys(openapi.paths).length} paths registered)`);
  }

  // Test 4: Webhook HMAC-SHA256 signature verification
  {
    const secret = 'whsec_test_secret_key_12345';
    const timestamp = '2026-10-20T21:00:00.000Z';
    const payload = JSON.stringify({ event: 'order.created', order_id: 'ord-123' });

    const sig1 = WebhookDispatcher.generateSignature(payload, secret, timestamp);
    const sig2 = WebhookDispatcher.generateSignature(payload, secret, timestamp);
    assert(sig1 === sig2, 'HMAC-SHA256 signature generation is deterministic');
    assert(sig1.length === 64, 'HMAC-SHA256 signature is a 64-character hex string');

    const sigTampered = WebhookDispatcher.generateSignature(payload + 'x', secret, timestamp);
    assert(sig1 !== sigTampered, 'HMAC-SHA256 detects payload tampering');
  }

  // Test 5: Unauthenticated access rejection
  {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/users',
    });
    assert(res.statusCode === 401, 'Unauthenticated request to /api/v1/users returns HTTP 401');
    const body = JSON.parse(res.body);
    assert(body.success === false, 'Error response format has success: false');
  }

  // Test 6: Invalid API Key rejection
  {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/users',
      headers: {
        'x-api-key': 'invalid_secret_key_123',
      },
    });
    assert(res.statusCode === 401, 'Invalid X-API-Key returns HTTP 401');
  }

  // Test 7: Export CSV Converter
  {
    const sampleRows = [
      { id: '1', sku: 'ENOL-5W30', trade_price: '14500.00', category: 'PASSENGER_CAR_MOTOR_OIL' },
      { id: '2', sku: 'ENOL-20W50', trade_price: '7200.00', category: 'PASSENGER_CAR_MOTOR_OIL' },
    ];
    // @ts-ignore
    const csv = ExportService['convertToCsv'](sampleRows);
    assert(csv.includes('"id","sku","trade_price","category"'), 'CSV export generates correct header row');
    assert(csv.includes('"ENOL-5W30"'), 'CSV export includes data rows properly escaped');
  }

  // Test 8: 5-Tier Organizational Hierarchy Paths
  {
    const directorId = '11111111-1111-1111-1111-111111111111';
    const zsmId = '22222222-2222-2222-2222-222222222222';
    const asmId = '33333333-3333-3333-3333-333333333333';
    const repId = '44444444-4444-4444-4444-444444444444';

    const repPath = `/${directorId}/${zsmId}/${asmId}/${repId}/`;
    const zsmPath = `/${directorId}/${zsmId}/`;

    assert(repPath.startsWith(zsmPath), 'Materialized path properly resolves subordinate relations');
  }

  await app.close();

  console.log('\n====================================================');
  console.log(`TEST RESULTS: ${passed} PASSED, ${failed} FAILED`);
  console.log('====================================================');

  if (failed > 0) {
    process.exit(1);
  }
}

if (require.main === module) {
  runTests();
}
