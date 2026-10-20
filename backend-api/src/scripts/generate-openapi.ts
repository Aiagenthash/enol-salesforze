import fs from 'fs';
import path from 'path';
import YAML from 'yaml';
import { buildApp } from '../app';

async function generateOpenApi() {
  console.log('--- Generating OpenAPI 3.1 YAML Specification ---');
  const app = buildApp();
  await app.ready();

  const openApiSpec = app.swagger();
  const yamlString = YAML.stringify(openApiSpec, { indent: 2 });

  const outputPath = path.resolve(__dirname, '../../../docs/openapi_v1.yaml');
  fs.writeFileSync(outputPath, yamlString, 'utf-8');

  console.log(`[SUCCESS] OpenAPI 3.1 specification written to: ${outputPath}`);
  process.exit(0);
}

if (require.main === module) {
  generateOpenApi();
}
