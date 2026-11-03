import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { pluralize, run } from '../scaffold-module.mjs';
import { check } from '../check-architecture.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const script = path.join(here, '..', 'scaffold-module.mjs');
const BASE = 'com.acme.app';

const FULL_POM = '<project><dependencies><artifactId>spring-boot-starter-web</artifactId><artifactId>spring-boot-starter-data-jpa</artifactId>' +
  '<artifactId>spring-boot-starter-validation</artifactId><artifactId>archunit-junit5</artifactId><artifactId>h2</artifactId></dependencies></project>';

function project(pom = FULL_POM) {
  const dir = mkdtempSync(path.join(os.tmpdir(), 'scaffold-'));
  writeFileSync(path.join(dir, 'pom.xml'), pom);
  mkdirSync(path.join(dir, 'src', 'main', 'java'), { recursive: true });
  return dir;
}

function files(dir) {
  const out = [];
  const walk = (d) => readdirSync(d).forEach((n) => (statSync(path.join(d, n)).isDirectory() ? walk(path.join(d, n)) : out.push(path.relative(dir, path.join(d, n)).split(path.sep).join('/'))));
  walk(dir);
  return out.sort();
}

const base = (project_, feature, extra = {}) => ({ project: project_, basePackage: BASE, feature, style: 'rich', web: true, shared: 'auto', dryRun: false, ...extra });

test('pluralize', () => {
  assert.equal(pluralize('order'), 'orders');
  assert.equal(pluralize('category'), 'categories');
  assert.equal(pluralize('address'), 'addresses');
  assert.equal(pluralize('day'), 'days');
});

test('rich: creates the layout of the model, replaces every token, and the result passes the checker', () => {
  const dir = project();
  const result = run(base(dir, 'order'));
  const p = `src/main/java/${BASE.replaceAll('.', '/')}`;
  const t = `src/test/java/${BASE.replaceAll('.', '/')}`;
  for (const expected of [
    `${p}/order/domain/Order.java`, `${p}/order/domain/OrderRepository.java`, `${p}/order/domain/OrderNotFoundException.java`, `${p}/order/domain/InvalidOrderException.java`,
    `${p}/order/application/OrderService.java`,
    `${p}/order/infra/persistence/OrderEntity.java`, `${p}/order/infra/persistence/OrderJpaRepository.java`, `${p}/order/infra/persistence/OrderRepositoryAdapter.java`,
    `${p}/order/infra/web/OrderController.java`, `${p}/order/infra/web/OrderResponse.java`,
    `${p}/shared/error/BusinessRuleException.java`, `${p}/shared/web/GlobalExceptionHandler.java`, `${p}/shared/config/ClockConfig.java`,
    `${t}/order/domain/OrderTest.java`, `${t}/order/application/OrderServiceTest.java`, `${t}/order/infra/persistence/OrderRepositoryAdapterTest.java`, `${t}/order/infra/web/OrderControllerTest.java`,
    `${t}/ArchitectureTest.java`,
  ]) assert.ok(existsSync(path.join(dir, expected)), `missing ${expected}`);
  assert.equal(result.created.length, files(dir).filter((f) => f.endsWith('.java')).length);
  for (const f of files(dir).filter((f) => f.endsWith('.java'))) assert.ok(!readFileSync(path.join(dir, f), 'utf-8').includes('{{'), `${f} still has a token`);
  assert.match(readFileSync(path.join(dir, `${p}/order/infra/persistence/OrderEntity.java`), 'utf-8'), /@Table\(name = "orders"\)/);
  assert.equal(check({ src: path.join(dir, 'src', 'main', 'java'), basePackage: BASE }).violations.length, 0);
  rmSync(dir, { recursive: true });
});

test('crud: a flat package with entity, repository, service, controller and records', () => {
  const dir = project();
  run(base(dir, 'category', { style: 'crud' }));
  const p = `src/main/java/${BASE.replaceAll('.', '/')}/category`;
  assert.deepEqual(files(dir).filter((f) => f.includes('/category/')).map((f) => path.posix.basename(f)).sort(),
    ['Category.java', 'CategoryController.java', 'CategoryControllerTest.java', 'CategoryRepository.java', 'CategoryRequest.java', 'CategoryResponse.java', 'CategoryService.java', 'CategoryServiceTest.java']);
  assert.match(readFileSync(path.join(dir, `${p}/Category.java`), 'utf-8'), /@Table\(name = "categories"\)/);
  assert.equal(check({ src: path.join(dir, 'src', 'main', 'java'), basePackage: BASE }).violations.length, 0);
  rmSync(dir, { recursive: true });
});

test('--no-web leaves out the controller, the records and the web test', () => {
  const dir = project();
  run(base(dir, 'order', { web: false }));
  assert.ok(!files(dir).some((f) => /Controller|Response|Request/.test(f) && !f.includes('GlobalExceptionHandler')));
  rmSync(dir, { recursive: true });
});

test('dry run writes nothing but reports the plan', () => {
  const dir = project();
  const result = run(base(dir, 'order', { dryRun: true }));
  assert.ok(result.created.length > 10);
  assert.deepEqual(files(dir), ['pom.xml']);
  rmSync(dir, { recursive: true });
});

test('a second feature reuses the shared kit and the architecture test (no duplicates)', () => {
  const dir = project();
  run(base(dir, 'order'));
  const second = run(base(dir, 'category', { style: 'crud' }));
  assert.ok(!second.created.some((f) => f.includes('/shared/') || f.endsWith('ArchitectureTest.java')));
  assert.ok(second.skipped.some((s) => s.file.endsWith('ArchitectureTest.java')));
  rmSync(dir, { recursive: true });
});

test('refuses to overwrite an existing feature and writes nothing', () => {
  const dir = project();
  run(base(dir, 'order'));
  const before = files(dir);
  assert.throws(() => run(base(dir, 'order')), /refusing to overwrite/);
  assert.deepEqual(files(dir), before);
  rmSync(dir, { recursive: true });
});

test('does not create a second exception handler when the project already has one', () => {
  const dir = project();
  const handler = path.join(dir, 'src/main/java/com/acme/app/config/HandlerException.java');
  mkdirSync(path.dirname(handler), { recursive: true });
  writeFileSync(handler, 'package com.acme.app.config;\n@RestControllerAdvice class HandlerException {}\n');
  const result = run(base(dir, 'order'));
  assert.ok(!result.created.some((f) => f.endsWith('GlobalExceptionHandler.java')));
  assert.ok(result.skipped.some((s) => s.file.endsWith('GlobalExceptionHandler.java')));
  rmSync(dir, { recursive: true });
});

test('reports the pom dependencies that are missing', () => {
  const dir = project('<project><dependencies><artifactId>spring-boot-starter-web</artifactId></dependencies></project>');
  const missing = run(base(dir, 'order', { dryRun: true })).missingDependencies.map((d) => d.artifactId);
  assert.deepEqual(missing.sort(), ['archunit-junit5', 'h2', 'spring-boot-starter-data-jpa', 'spring-boot-starter-validation']);
  assert.deepEqual(run(base(project(), 'order', { dryRun: true })).missingDependencies, []);
  rmSync(dir, { recursive: true });
});

test('rejects invalid input', () => {
  const dir = project();
  assert.throws(() => run(base(dir, 'Order')), /invalid feature name/);
  assert.throws(() => run(base(dir, 'my-order')), /invalid feature name/);
  assert.throws(() => run(base(dir, 'service')), /reserved/);
  assert.throws(() => run(base(dir, 'order', { entity: 'order' })), /PascalCase/);
  assert.throws(() => run(base(dir, 'order', { style: 'weird' })), /style/);
  assert.throws(() => run({ ...base(dir, 'order'), basePackage: 'Com.Acme' }), /invalid base package/);
  assert.throws(() => run(base(path.join(dir, 'nope'), 'order')), /no pom.xml/);
  rmSync(dir, { recursive: true });
});

test('CLI: exit 0 with --json, exit 2 on a conflict', () => {
  const dir = project();
  const args = [script, '--project', dir, '--base-package', BASE, '--feature', 'order', '--json'];
  const ok = spawnSync(process.execPath, args, { encoding: 'utf-8' });
  assert.equal(ok.status, 0, ok.stderr);
  assert.ok(JSON.parse(ok.stdout).created.length > 10);
  const again = spawnSync(process.execPath, args, { encoding: 'utf-8' });
  assert.equal(again.status, 2);
  assert.match(again.stderr, /refusing to overwrite/);
  assert.equal(spawnSync(process.execPath, [script, '--feature', 'order'], { encoding: 'utf-8' }).status, 2);
  rmSync(dir, { recursive: true });
});
