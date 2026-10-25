#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import YAML from 'yaml';

const skillRoot = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const redocly = path.join(skillRoot, 'node_modules', '@redocly', 'cli', 'bin', 'cli.js');
const httpMethods = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'trace'];

function run(args) {
  const result = spawnSync(process.execPath, [redocly, ...args], { encoding: 'utf8', maxBuffer: 20 * 1024 * 1024 });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${args[0]} falhou:\n${result.stdout}\n${result.stderr}`);
}

function parse(file) {
  const document = YAML.parseDocument(readFileSync(file, 'utf8'), { uniqueKeys: true });
  if (document.errors.length) throw new Error(`${file}: ${document.errors.map(error => error.message).join('; ')}`);
  return document.toJS();
}

function media(content) {
  return Object.entries(content ?? {}).map(([type, value]) => ({
    mediaType: type,
    schema: value?.schema?.$ref ?? value?.schema?.type ?? null
  }));
}

function summarize(document) {
  const operations = [];
  for (const [route, pathItem] of Object.entries(document.paths ?? {})) {
    for (const method of httpMethods) {
      const operation = pathItem?.[method];
      if (!operation) continue;
      operations.push({
        method: method.toUpperCase(),
        path: route,
        operationId: operation.operationId ?? null,
        tags: operation.tags ?? [],
        summary: operation.summary ?? null,
        parameters: [...(pathItem.parameters ?? []), ...(operation.parameters ?? [])].map(parameter => ({
          name: parameter.name ?? null,
          in: parameter.in ?? null,
          required: parameter.required ?? false,
          schema: parameter.schema?.$ref ?? parameter.schema?.type ?? parameter.$ref ?? null
        })),
        requestBody: operation.requestBody?.$ref ?? media(operation.requestBody?.content),
        responses: Object.fromEntries(Object.entries(operation.responses ?? {}).map(([status, response]) => [status, {
          description: response.description ?? null,
          content: media(response.content),
          headers: Object.keys(response.headers ?? {})
        }])),
        security: operation.security ?? document.security ?? null
      });
    }
  }
  return operations;
}

function select(operations, selector) {
  if (!selector) return { status: 'selection_required', matchedBy: null, operations };
  const query = selector.trim().toLowerCase();
  const methodPath = /^(get|post|put|patch|delete|head|options|trace)\s+(\/\S+)$/i.exec(selector.trim());
  if (methodPath) {
    const matches = operations.filter(op => op.method.toLowerCase() === methodPath[1].toLowerCase() && op.path.toLowerCase() === methodPath[2].toLowerCase());
    return { status: matches.length ? 'selected' : 'not_found', matchedBy: 'method_path', operations: matches };
  }
  for (const [kind, predicate] of [
    ['operationId', op => op.operationId?.toLowerCase() === query],
    ['tag', op => op.tags.some(tag => tag.toLowerCase() === query)],
    ['path', op => op.path.toLowerCase() === query]
  ]) {
    const matches = operations.filter(predicate);
    if (matches.length) return { status: 'selected', matchedBy: kind, operations: matches };
  }
  const matches = operations.filter(op => op.path.toLowerCase().includes(query) || op.operationId?.toLowerCase().includes(query) || op.tags.some(tag => tag.toLowerCase().includes(query)));
  return { status: matches.length === 0 ? 'not_found' : 'ambiguous', matchedBy: 'partial', operations: matches };
}

function inspect(entry, resource) {
  if (!existsSync(redocly)) throw new Error(`Dependências ausentes. Execute npm ci em ${skillRoot}`);
  if (!existsSync(entry)) throw new Error(`Contrato ausente: ${entry}`);
  const source = parse(entry);
  if (!/^3\.1\.\d+$/.test(String(source.openapi ?? ''))) throw new Error('Esta skill espera OpenAPI 3.1.x');
  const temp = mkdtempSync(path.join(os.tmpdir(), 'java-module-contract-'));
  try {
    const bundled = path.join(temp, 'openapi.bundle.yaml');
    run(['lint', '--extends=spec', entry]);
    run(['bundle', entry, '--output', bundled]);
    const resolved = parse(bundled);
    const all = summarize(resolved);
    return { entry, title: resolved.info?.title ?? null, version: resolved.info?.version ?? null, totalOperations: all.length, ...select(all, resource), available: resource ? all.map(op => `${op.method} ${op.path} (${op.operationId ?? 'sem operationId'})`) : undefined };
  } finally {
    rmSync(temp, { recursive: true, force: true });
  }
}

try {
  const args = process.argv.slice(2);
  const entryIndex = args.indexOf('--entry');
  const resourceIndex = args.indexOf('--resource');
  if (entryIndex < 0 || !args[entryIndex + 1] || (resourceIndex >= 0 && !args[resourceIndex + 1])) {
    throw new Error('Uso: node inspect-contract.mjs --entry <openapi.yaml> [--resource <tag|operationId|METHOD /path>]');
  }
  const result = inspect(path.resolve(args[entryIndex + 1]), resourceIndex >= 0 ? args[resourceIndex + 1] : null);
  console.log(JSON.stringify(result, null, 2));
  if (result.status === 'not_found') process.exitCode = 2;
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
