import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { check, stripCode } from '../check-architecture.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const script = path.join(here, '..', 'check-architecture.mjs');
const exampleSrc = path.join(here, '..', '..', 'examples', 'shop', 'src', 'main', 'java');
const BASE = 'com.acme.app';

/** Builds a throw-away source tree: files = { 'order/domain/Order.java': 'java body without package line' } */
function tree(files) {
  const src = mkdtempSync(path.join(os.tmpdir(), 'arch-'));
  for (const [rel, body] of Object.entries(files)) {
    const file = path.join(src, ...BASE.split('.'), ...rel.split('/'));
    mkdirSync(path.dirname(file), { recursive: true });
    const pkg = `${BASE}.${path.posix.dirname(rel).split('/').join('.')}`;
    writeFileSync(file, `package ${pkg};\n\n${body}\n`);
  }
  return src;
}

const rules = (result) => result.violations.map((v) => v.rule);

test('the compiled example project has zero violations (and no warnings)', () => {
  const result = check({ src: exampleSrc, basePackage: 'com.acme.shop' });
  assert.deepEqual(result.violations, []);
  assert.deepEqual(result.warnings, []);
  assert.ok(result.files > 20);
});

test('A01 domain imports a framework class (also fully qualified, never in comments)', () => {
  const src = tree({
    'order/domain/Order.java': 'import org.springframework.stereotype.Service;\nclass Order {}',
    'order/domain/Other.java': 'class Other { @jakarta.persistence.Id Long id; }',
    'order/domain/Fine.java': '// jakarta.persistence.Entity is only mentioned here\n/* org.slf4j.Logger too */\nclass Fine { String s = "jakarta.persistence.Id"; }',
  });
  const result = check({ src, basePackage: BASE });
  assert.deepEqual(rules(result), ['A01', 'A01']);
  assert.deepEqual(result.violations.map((v) => v.file).sort(), [`${BASE.replaceAll('.', '/')}/order/domain/Order.java`, `${BASE.replaceAll('.', '/')}/order/domain/Other.java`].sort());
  rmSync(src, { recursive: true });
});

test('A01 forbids Lombok and SLF4J in the domain', () => {
  const src = tree({ 'order/domain/Order.java': 'import lombok.Getter;\nimport org.slf4j.Logger;\nclass Order {}' });
  assert.deepEqual(rules(check({ src, basePackage: BASE })), ['A01', 'A01']);
  rmSync(src, { recursive: true });
});

test('A02 domain depends on application or infra', () => {
  const src = tree({
    'order/domain/Order.java': `import ${BASE}.order.application.OrderService;\nimport ${BASE}.order.infra.persistence.OrderEntity;\nclass Order {}`,
    'order/application/OrderService.java': 'class OrderService {}',
    'order/infra/persistence/OrderEntity.java': 'class OrderEntity {}',
  });
  assert.deepEqual(rules(check({ src, basePackage: BASE })), ['A02', 'A02']);
  rmSync(src, { recursive: true });
});

test('A03 application depends on infra or web/persistence technology', () => {
  const src = tree({
    'order/application/OrderService.java': `import ${BASE}.order.infra.persistence.OrderEntity;\nimport jakarta.persistence.EntityManager;\nimport org.springframework.http.ResponseEntity;\nimport org.springframework.stereotype.Service;\nimport org.springframework.transaction.annotation.Transactional;\nclass OrderService {}`,
    'order/infra/persistence/OrderEntity.java': 'class OrderEntity {}',
  });
  const result = check({ src, basePackage: BASE });
  assert.deepEqual(rules(result), ['A03', 'A03', 'A03']); // @Service and @Transactional imports are allowed
  rmSync(src, { recursive: true });
});

test('A04 web must not use persistence', () => {
  const src = tree({
    'order/infra/web/OrderController.java': `import ${BASE}.order.infra.persistence.OrderJpaRepository;\nclass OrderController {}`,
    'order/infra/persistence/OrderJpaRepository.java': 'interface OrderJpaRepository {}',
  });
  assert.deepEqual(rules(check({ src, basePackage: BASE })), ['A04']);
  rmSync(src, { recursive: true });
});

test('A05 a feature must not use another feature\'s infra, but may use its application and domain', () => {
  const src = tree({
    'invoice/application/InvoiceService.java': `import ${BASE}.order.application.OrderService;\nimport ${BASE}.order.domain.Order;\nimport ${BASE}.order.infra.persistence.OrderEntity;\nclass InvoiceService {}`,
    'order/application/OrderService.java': 'class OrderService {}',
    'order/domain/Order.java': 'class Order {}',
    'order/infra/persistence/OrderEntity.java': 'class OrderEntity {}',
  });
  const result = check({ src, basePackage: BASE });
  assert.deepEqual(rules(result), ['A03', 'A05']); // the same import also breaks A03 (application -> infra)
  assert.match(result.violations.find((v) => v.rule === 'A05').message, /feature 'invoice' uses infra of feature 'order'/);
  rmSync(src, { recursive: true });
});

test('A06 application declares an interface (annotation types and nested interfaces of records are not flagged by name)', () => {
  const src = tree({ 'order/application/OrderUseCase.java': 'public interface OrderUseCase { void run(); }' });
  const result = check({ src, basePackage: BASE });
  assert.deepEqual(rules(result), ['A06']);
  rmSync(src, { recursive: true });
});

test('A07 stereotypes of other layers are not allowed in domain/application', () => {
  const src = tree({
    'order/domain/Order.java': '@Entity class Order {}',
    'order/application/OrderService.java': '@RestController @Repository class OrderService {}',
  });
  assert.deepEqual(rules(check({ src, basePackage: BASE })), ['A07', 'A07', 'A07']);
  rmSync(src, { recursive: true });
});

test('A08 @Transactional not in domain or web, fine in application', () => {
  const src = tree({
    'order/domain/Order.java': 'class Order { @Transactional void x() {} }',
    'order/infra/web/OrderController.java': '@Transactional class OrderController {}',
    'order/application/OrderService.java': '@Transactional class OrderService {}',
  });
  assert.deepEqual(rules(check({ src, basePackage: BASE })), ['A08', 'A08']);
  rmSync(src, { recursive: true });
});

test('A09 no @Autowired anywhere', () => {
  const src = tree({ 'category/CategoryService.java': 'class CategoryService { @Autowired Object repo; }' });
  assert.deepEqual(rules(check({ src, basePackage: BASE })), ['A09']);
  rmSync(src, { recursive: true });
});

test('A10 shared depends on no feature', () => {
  const src = tree({
    'shared/config/X.java': `import ${BASE}.order.domain.Order;\nclass X {}`,
    'order/domain/Order.java': 'class Order {}',
  });
  assert.deepEqual(rules(check({ src, basePackage: BASE })), ['A10']);
  rmSync(src, { recursive: true });
});

test('A11 a rich feature keeps @Entity in infra.persistence; a flat crud feature may keep it in its package', () => {
  const rich = tree({
    'order/domain/Order.java': 'class Order {}',
    'order/infra/web/OrderEntity.java': '@Entity class OrderEntity {}',
    'order/infra/persistence/OrderRow.java': '@Entity class OrderRow {}',
  });
  assert.deepEqual(rules(check({ src: rich, basePackage: BASE })), ['A11']);
  const crud = tree({ 'category/Category.java': '@Entity class Category {}' });
  assert.deepEqual(rules(check({ src: crud, basePackage: BASE })), []);
  rmSync(rich, { recursive: true });
  rmSync(crud, { recursive: true });
});

test('warnings for class-per-use-case and Impl classes in application', () => {
  const src = tree({
    'order/application/CreateOrderUseCase.java': 'class CreateOrderUseCase {}',
    'order/application/OrderServiceImpl.java': 'class OrderServiceImpl {}',
  });
  const result = check({ src, basePackage: BASE });
  assert.deepEqual(result.violations, []);
  assert.deepEqual(result.warnings.map((w) => w.rule).sort(), ['W01', 'W02']);
  rmSync(src, { recursive: true });
});

test('legacy packages are ignored while migrating and fail in strict mode', () => {
  const src = tree({
    'controllers/OrderController.java': '@Entity @Autowired class OrderController {}',
    'order/domain/Order.java': 'class Order {}',
  });
  const lenient = check({ src, basePackage: BASE, legacy: ['controllers'] });
  assert.deepEqual(lenient.violations, []);
  assert.equal(lenient.legacy.files.length, 1);
  const strict = check({ src, basePackage: BASE, legacy: ['controllers'], strict: true });
  assert.deepEqual(rules(strict), ['LEGACY']);
  rmSync(src, { recursive: true });
});

test('line numbers point at the offending line', () => {
  const src = tree({ 'order/domain/Order.java': '// header\n// more\nimport org.springframework.stereotype.Service;\nclass Order {}' });
  assert.equal(check({ src, basePackage: BASE }).violations[0].line, 5);
  rmSync(src, { recursive: true });
});

test('stripCode keeps the number of lines', () => {
  const source = 'a\n/* x\ny */\n"s\\"t" // c\nb';
  assert.equal(stripCode(source).split('\n').length, source.split('\n').length);
});

test('CLI: exit 0 on the example, 1 on violations, 2 on usage errors', () => {
  const ok = spawnSync(process.execPath, [script, '--src', exampleSrc, '--base-package', 'com.acme.shop'], { encoding: 'utf-8' });
  assert.equal(ok.status, 0, ok.stdout + ok.stderr);
  assert.match(ok.stdout, /0 violation/);

  const src = tree({ 'order/domain/Order.java': 'import lombok.Data;\nclass Order {}' });
  const bad = spawnSync(process.execPath, [script, '--src', src, '--base-package', BASE, '--json'], { encoding: 'utf-8' });
  assert.equal(bad.status, 1);
  assert.equal(JSON.parse(bad.stdout).violations[0].rule, 'A01');
  rmSync(src, { recursive: true });

  assert.equal(spawnSync(process.execPath, [script], { encoding: 'utf-8' }).status, 2);
  assert.equal(spawnSync(process.execPath, [script, '--src', '/nope', '--base-package', 'x.y'], { encoding: 'utf-8' }).status, 2);
});
