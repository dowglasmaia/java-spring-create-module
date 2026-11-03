#!/usr/bin/env node
// Deterministic skeleton generator for one feature module ("Hexagonal Pragmática"). Standard library only.
// Usage: node scaffold-module.mjs --project <dir> --base-package <pkg> --feature <name> [--entity <Name>] [--style rich|crud]
//        [--table <name>] [--no-web] [--shared auto|yes|no] [--dry-run] [--json]
// Exit: 0 ok, 2 usage error or conflict (nothing is written in that case).
import { existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const skillRoot = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const templates = path.join(skillRoot, 'templates');
const RESERVED = new Set(['shared', 'domain', 'application', 'infra', 'config', 'util', 'utils', 'common', 'core', 'web', 'service', 'controller', 'entity', 'repository']);

class UsageError extends Error {}

export function parseArgs(argv) {
  const options = { style: 'rich', web: true, shared: 'auto', dryRun: false, json: false };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    const next = () => {
      if (i + 1 >= argv.length) throw new UsageError(`${arg} needs a value`);
      return argv[++i];
    };
    if (arg === '--project') options.project = next();
    else if (arg === '--base-package') options.basePackage = next();
    else if (arg === '--feature') options.feature = next();
    else if (arg === '--entity') options.entity = next();
    else if (arg === '--style') options.style = next();
    else if (arg === '--table') options.table = next();
    else if (arg === '--shared') options.shared = next();
    else if (arg === '--no-web') options.web = false;
    else if (arg === '--dry-run') options.dryRun = true;
    else if (arg === '--json') options.json = true;
    else throw new UsageError(`unknown argument: ${arg}`);
  }
  return options;
}

export function pluralize(word) {
  if (/[^aeiou]y$/.test(word)) return `${word.slice(0, -1)}ies`;
  if (/(s|x|z|ch|sh)$/.test(word)) return `${word}es`;
  return `${word}s`;
}

const pascal = (s) => s.charAt(0).toUpperCase() + s.slice(1);
const camel = (s) => s.charAt(0).toLowerCase() + s.slice(1);
const snake = (s) => s.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();

function validate(options) {
  if (!options.project) throw new UsageError('--project is required');
  if (!options.basePackage) throw new UsageError('--base-package is required');
  if (!options.feature) throw new UsageError('--feature is required (the business capability, e.g. order)');
  if (!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$/.test(options.basePackage)) throw new UsageError(`invalid base package: ${options.basePackage}`);
  if (!/^[a-z][a-z0-9]*$/.test(options.feature)) throw new UsageError(`invalid feature name "${options.feature}": lower-case letters and digits, no separators`);
  if (RESERVED.has(options.feature)) throw new UsageError(`"${options.feature}" is a reserved name; name the feature by its business capability`);
  if (!['rich', 'crud'].includes(options.style)) throw new UsageError('--style must be rich or crud');
  if (!['auto', 'yes', 'no'].includes(options.shared)) throw new UsageError('--shared must be auto, yes or no');
  if (options.entity && !/^[A-Z][A-Za-z0-9]*$/.test(options.entity)) throw new UsageError(`invalid entity name "${options.entity}": PascalCase`);
  const pom = path.join(options.project, 'pom.xml');
  if (!existsSync(pom)) throw new UsageError(`no pom.xml in ${options.project}`);
  if (!existsSync(path.join(options.project, 'src', 'main', 'java'))) throw new UsageError(`no src/main/java in ${options.project}`);
}

function walk(dir) {
  const out = [];
  if (!existsSync(dir)) return out;
  for (const name of readdirSync(dir)) {
    const full = path.join(dir, name);
    if (statSync(full).isDirectory()) out.push(...walk(full));
    else out.push(full);
  }
  return out;
}

function render(text, tokens) {
  let out = text;
  for (const [key, value] of Object.entries(tokens)) out = out.split(`{{${key}}}`).join(value);
  return out;
}

export function plan(options) {
  validate(options);
  const entity = options.entity ?? pascal(options.feature);
  const tokens = {
    BASE: options.basePackage,
    BASE_PATH: options.basePackage.split('.').join('/'),
    feature: options.feature,
    Entity: entity,
    entity: camel(entity),
    table: options.table ?? pluralize(snake(entity)),
  };
  const basePath = options.basePackage.split('.').join('/');
  const mainRoot = path.join(options.project, 'src', 'main', 'java', basePath);
  const testRoot = path.join(options.project, 'src', 'test', 'java', basePath);

  const groups = options.style === 'rich'
    ? [['rich/main', mainRoot], ...(options.web ? [['rich/web', mainRoot]] : []), ['rich/test', testRoot], ...(options.web ? [['rich/webtest', testRoot]] : [])]
    : [['crud/main', mainRoot], ...(options.web ? [['crud/web', mainRoot]] : []), ['crud/test', testRoot], ...(options.web ? [['crud/webtest', testRoot]] : [])];

  const featureFiles = [];
  for (const [group, root] of groups) {
    const groupDir = path.join(templates, ...group.split('/'));
    for (const file of walk(groupDir)) {
      const rel = path.relative(groupDir, file).split(path.sep).join('/')
        .replace(/__feature__/g, tokens.feature).replace(/__Entity__/g, tokens.Entity).replace(/\.tpl$/, '');
      featureFiles.push({ kind: 'feature', template: file, target: path.join(root, ...rel.split('/')) });
    }
  }

  const ensureFiles = [];
  const mainJava = path.join(options.project, 'src', 'main', 'java');
  const hasAdvice = walk(mainJava).some((f) => f.endsWith('.java') && /@RestControllerAdvice\b/.test(readFileSync(f, 'utf-8')));
  if (options.shared !== 'no') {
    const sharedDir = path.join(templates, 'shared', 'main');
    for (const file of walk(sharedDir)) {
      const rel = path.relative(sharedDir, file).split(path.sep).join('/').replace(/\.tpl$/, '');
      const isHandler = rel.endsWith('GlobalExceptionHandler.java');
      const skipHandler = isHandler && options.shared === 'auto' && hasAdvice;
      ensureFiles.push({ kind: 'shared', template: file, target: path.join(mainRoot, ...rel.split('/')), skip: skipHandler ? 'the project already has a @RestControllerAdvice' : null });
    }
  }
  const hasArchTest = walk(path.join(options.project, 'src', 'test', 'java')).some((f) => f.endsWith('.java') && /@AnalyzeClasses\b|com\.tngtech\.archunit/.test(readFileSync(f, 'utf-8')));
  ensureFiles.push({ kind: 'architecture-test', template: path.join(templates, 'ArchitectureTest.java.tpl'), target: path.join(testRoot, 'ArchitectureTest.java'), skip: hasArchTest ? 'an ArchUnit test already exists' : null });

  const featureDir = path.join(mainRoot, tokens.feature);
  const conflicts = [];
  if (existsSync(featureDir) && walk(featureDir).length > 0) conflicts.push(`${path.relative(options.project, featureDir)} already has files (use migrate or review for an existing feature)`);
  for (const f of featureFiles) if (existsSync(f.target)) conflicts.push(`${path.relative(options.project, f.target)} already exists`);

  return { tokens, featureFiles, ensureFiles, conflicts };
}

function missingDependencies(options) {
  const pom = readFileSync(path.join(options.project, 'pom.xml'), 'utf-8');
  const need = [
    ['spring-boot-starter-web', 'org.springframework.boot', null, 'compile'],
    ['spring-boot-starter-data-jpa', 'org.springframework.boot', null, 'compile'],
    ['spring-boot-starter-validation', 'org.springframework.boot', null, 'compile'],
    ['archunit-junit5', 'com.tngtech.archunit', '1.3.0', 'test'],
  ];
  const missing = need.filter(([artifact]) => !pom.includes(artifact)).map(([artifactId, groupId, version, scope]) => ({ groupId, artifactId, version, scope }));
  if (options.style === 'rich' && !/<artifactId>h2<\/artifactId>|testcontainers/.test(pom)) {
    missing.push({ groupId: 'com.h2database', artifactId: 'h2', version: null, scope: 'test', note: 'embedded DB for the @DataJpaTest adapter test (not needed if the project tests with Testcontainers)' });
  }
  return missing;
}

export function run(options) {
  const p = plan(options);
  if (p.conflicts.length > 0) throw new UsageError(`refusing to overwrite:\n  - ${p.conflicts.join('\n  - ')}`);
  const created = [];
  const skipped = [];
  const write = (f) => {
    const relative = path.relative(options.project, f.target).split(path.sep).join('/');
    if (f.skip) { skipped.push({ file: relative, reason: f.skip }); return; }
    if (existsSync(f.target)) { skipped.push({ file: relative, reason: 'already exists' }); return; }
    created.push(relative);
    if (options.dryRun) return;
    mkdirSync(path.dirname(f.target), { recursive: true });
    writeFileSync(f.target, render(readFileSync(f.template, 'utf-8'), p.tokens), 'utf-8');
  };
  [...p.featureFiles, ...p.ensureFiles].forEach(write);
  const checkCommand = `node ${path.join(skillRoot, 'scripts', 'check-architecture.mjs')} --src ${path.join(options.project, 'src', 'main', 'java')} --base-package ${options.basePackage}`;
  return {
    dryRun: options.dryRun,
    style: options.style,
    tokens: p.tokens,
    created,
    skipped,
    missingDependencies: missingDependencies(options),
    nextSteps: [
      'Add the missing dependencies (if any) to pom.xml.',
      'Replace every TODO(domain) / TODO(contract) marker: tests first for each rule.',
      `Run the checker: ${checkCommand}`,
      'Run the project build (./mvnw -B verify) including ArchitectureTest.',
    ],
  };
}

function main() {
  let options;
  try {
    options = parseArgs(process.argv.slice(2));
    const result = run(options);
    if (options.json) {
      console.log(JSON.stringify(result, null, 2));
    } else {
      console.log(`${result.dryRun ? '[dry run] would create' : 'created'} ${result.created.length} file(s) (${result.style}):`);
      result.created.forEach((f) => console.log(`  + ${f}`));
      result.skipped.forEach((s) => console.log(`  = ${s.file} (skipped: ${s.reason})`));
      if (result.missingDependencies.length > 0) {
        console.log('\nmissing pom.xml dependencies:');
        for (const d of result.missingDependencies) console.log(`  <dependency><groupId>${d.groupId}</groupId><artifactId>${d.artifactId}</artifactId>${d.version ? `<version>${d.version}</version>` : ''}${d.scope === 'test' ? '<scope>test</scope>' : ''}</dependency>${d.note ? `  <!-- ${d.note} -->` : ''}`);
      }
      console.log('\nnext:');
      result.nextSteps.forEach((s, i) => console.log(`  ${i + 1}. ${s}`));
    }
  } catch (error) {
    console.error(`error: ${error.message}`);
    if (error instanceof UsageError) process.exit(2);
    throw error;
  }
}

if (process.argv[1]?.endsWith('scaffold-module.mjs')) main();
