#!/usr/bin/env node
// Offline checker for the "Hexagonal Pragmática" rules (A01-A12 + warnings). Standard library only.
// Usage: node check-architecture.mjs --src <src/main/java> --base-package <pkg> [--legacy a,b,c] [--strict] [--json]
// Exit: 0 no violations, 1 violations, 2 usage error.
import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

const FRAMEWORK_PREFIXES = [
  'org.springframework.', 'jakarta.', 'javax.', 'com.fasterxml.', 'org.hibernate.', 'lombok.',
  'org.slf4j.', 'org.apache.kafka.', 'feign.',
];
const APPLICATION_FORBIDDEN = [
  'jakarta.persistence.', 'jakarta.servlet.', 'org.springframework.web.', 'org.springframework.http.',
  'org.springframework.data.', 'org.springframework.jdbc.', 'org.springframework.kafka.', 'org.springframework.cloud.',
];
const LAYERS = new Set(['domain', 'application', 'api', 'infrastructure']);
const MAX_FLAT_PACKAGE = 12;
const OUTER = new Set(['application', 'api', 'infrastructure']);

export function parseArgs(argv) {
  const options = { legacy: [], strict: false, json: false };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--src') options.src = argv[++i];
    else if (arg === '--base-package') options.basePackage = argv[++i];
    else if (arg === '--legacy') options.legacy = (argv[++i] ?? '').split(',').map((s) => s.trim()).filter(Boolean);
    else if (arg === '--strict') options.strict = true;
    else if (arg === '--json') options.json = true;
    else throw new Error(`unknown argument: ${arg}`);
  }
  if (!options.src) throw new Error('--src is required');
  if (!options.basePackage) throw new Error('--base-package is required');
  return options;
}

function walk(dir) {
  const files = [];
  for (const name of readdirSync(dir)) {
    const full = path.join(dir, name);
    if (statSync(full).isDirectory()) files.push(...walk(full));
    else if (name.endsWith('.java')) files.push(full);
  }
  return files;
}

/** Blanks comments and string/char literals but keeps every newline, so line numbers stay exact. */
export function stripCode(source) {
  let out = '';
  let i = 0;
  const blank = (text) => text.replace(/[^\n]/g, ' ');
  while (i < source.length) {
    const two = source.slice(i, i + 2);
    if (two === '//') {
      const end = source.indexOf('\n', i);
      const stop = end < 0 ? source.length : end;
      out += blank(source.slice(i, stop));
      i = stop;
    } else if (two === '/*') {
      const end = source.indexOf('*/', i + 2);
      const stop = end < 0 ? source.length : end + 2;
      out += blank(source.slice(i, stop));
      i = stop;
    } else if (source.startsWith('"""', i)) {
      const end = source.indexOf('"""', i + 3);
      const stop = end < 0 ? source.length : end + 3;
      out += blank(source.slice(i, stop));
      i = stop;
    } else if (source[i] === '"' || source[i] === "'") {
      const quote = source[i];
      let j = i + 1;
      while (j < source.length && source[j] !== quote && source[j] !== '\n') j += source[j] === '\\' ? 2 : 1;
      out += blank(source.slice(i, j + 1));
      i = j + 1;
    } else {
      out += source[i];
      i++;
    }
  }
  return out;
}

function lineOf(code, index) {
  let line = 1;
  for (let i = 0; i < index; i++) if (code.charCodeAt(i) === 10) line++;
  return line;
}

function parseFile(file, source) {
  const code = stripCode(source);
  const pkg = /^\s*package\s+([\w.]+)\s*;/m.exec(code)?.[1] ?? '';
  const imports = [];
  const importRe = /^\s*import\s+(?:static\s+)?([\w.*]+)\s*;/gm;
  for (let m = importRe.exec(code); m; m = importRe.exec(code)) imports.push({ name: m[1], line: lineOf(code, m.index + m[0].search(/\S/)) });
  return { file, code, pkg, imports };
}

export function check({ src, basePackage, legacy = [], strict = false }) {
  const violations = [];
  const warnings = [];
  const legacySet = new Set(legacy);
  const legacyFiles = [];
  const base = `${basePackage}.`;
  const root = path.join(src, ...basePackage.split('.'));
  let files;
  try {
    files = walk(root);
  } catch {
    throw new Error(`base package folder not found: ${root}`);
  }

  const parsed = files.map((file) => parseFile(file, readFileSync(file, 'utf-8')));
  const describe = (p) => {
    if (!p.pkg.startsWith(base)) return { feature: null, layer: null, sub: null };
    const seg = p.pkg.slice(base.length).split('.');
    const layer = LAYERS.has(seg[1]) ? seg[1] : null;
    return { feature: seg[0], layer, sub: layer === 'infrastructure' || layer === 'application' ? seg[2] ?? null : null };
  };
  const featuresWithDomain = new Set(parsed.filter((p) => describe(p).layer === 'domain').map((p) => describe(p).feature));

  const add = (list, rule, p, line, message) => list.push({ rule, file: path.relative(src, p.file).split(path.sep).join('/'), line, message });
  const importFeature = (name) => {
    if (!name.startsWith(base)) return null;
    const rest = name.slice(base.length).split('.');
    return { feature: rest[0], layer: rest[1] ?? null, sub: rest[2] ?? null };
  };

  for (const p of parsed) {
    const { feature, layer, sub } = describe(p);
    if (feature && legacySet.has(feature)) {
      legacyFiles.push(path.relative(src, p.file).split(path.sep).join('/'));
      continue;
    }
    const code = p.code;
    const annotation = (name) => new RegExp(`@${name}\\b`).exec(code);

    for (const imp of p.imports) {
      const target = importFeature(imp.name);
      if (layer === 'domain') {
        if (FRAMEWORK_PREFIXES.some((prefix) => imp.name.startsWith(prefix))) add(violations, 'A01', p, imp.line, `domain imports framework class ${imp.name}`);
        if (target && OUTER.has(target.layer)) add(violations, 'A02', p, imp.line, `domain depends on ${target.layer}: ${imp.name}`);
      }
      if (layer === 'application') {
        if (target && (target.layer === 'infrastructure' || target.layer === 'api')) add(violations, 'A03', p, imp.line, `application depends on ${target.layer}: ${imp.name}`);
        else if (APPLICATION_FORBIDDEN.some((prefix) => imp.name.startsWith(prefix))) add(violations, 'A03', p, imp.line, `application imports technology class ${imp.name}`);
      }
      if (layer === 'api' && target && target.layer === 'infrastructure') {
        add(violations, 'A04', p, imp.line, `api depends on infrastructure: ${imp.name}`);
      }
      if (feature && feature !== 'shared' && target && target.feature !== feature && target.feature !== 'shared' && (target.layer === 'infrastructure' || target.layer === 'api')) {
        add(violations, 'A05', p, imp.line, `feature '${feature}' uses ${target.layer} of feature '${target.feature}': ${imp.name}`);
      }
      if (feature === 'shared' && target && target.feature !== 'shared' && !legacySet.has(target.feature)) {
        add(violations, 'A10', p, imp.line, `shared depends on feature '${target.feature}': ${imp.name}`);
      }
    }

    // fully qualified use without an import (domain only)
    if (layer === 'domain') {
      const fq = /\b(org\.springframework|jakarta|javax|com\.fasterxml|org\.hibernate|lombok|org\.slf4j|org\.apache\.kafka)\.[\w.]+/.exec(code.replace(/^\s*import\s[^;]+;/gm, (s) => s.replace(/[^\n]/g, ' ')));
      if (fq) add(violations, 'A01', p, lineOf(code, fq.index), `domain uses framework class ${fq[0]}`);
    }

    if (layer === 'application') {
      const decl = /^\s*(?:public\s+|abstract\s+|final\s+|sealed\s+|non-sealed\s+)*(interface|class|record|enum)\s+(\w+)/m.exec(code);
      const at = decl ? lineOf(code, decl.index + decl[0].search(/\S/)) : 1;
      if (sub === 'port') {
        if (decl && decl[1] !== 'interface') add(violations, 'A06', p, at, `application.port holds interfaces only (found ${decl[1]} ${decl[2]}); records and enums belong in domain`);
      } else if (sub === 'usecase') {
        if (decl && decl[1] === 'interface') add(violations, 'A06', p, at, `application.usecase declares an interface (${decl[2]}): the use case is a concrete class; ports live in application.port`);
        if (decl && decl[1] === 'class' && !/UseCase$/.test(decl[2])) add(violations, 'A12', p, at, `use case class must be named <Verb><Noun>UseCase (found ${decl[2]})`);
        if (decl && decl[1] === 'class' && /Impl$/.test(decl[2])) add(warnings, 'W02', p, at, `implementation class without interface need: ${decl[2]}`);
      } else if (decl) {
        add(warnings, 'W01', p, at, `${decl[2]} sits directly in application: use cases go to application.usecase, ports to application.port`);
      }
    }
    if (layer !== 'application' && /\bclass\s+\w+UseCase\b/.test(code)) {
      const m = /\bclass\s+\w+UseCase\b/.exec(code);
      add(violations, 'A12', p, lineOf(code, m.index), 'a *UseCase class must live in application.usecase');
    }

    if (layer === 'domain' || layer === 'application') {
      for (const name of ['RestController', 'Controller', 'RestControllerAdvice', 'Repository', 'Entity', 'Table']) {
        const m = annotation(name);
        if (m) add(violations, 'A07', p, lineOf(code, m.index), `@${name} is not allowed in ${layer}`);
      }
    }
    if (layer === 'api') {
      for (const name of ['Repository', 'Entity', 'Table']) {
        const m = annotation(name);
        if (m) add(violations, 'A07', p, lineOf(code, m.index), `@${name} is not allowed in api`);
      }
    }
    if (layer === 'domain' || layer === 'api') {
      const m = annotation('Transactional');
      if (m) add(violations, 'A08', p, lineOf(code, m.index), `@Transactional is not allowed in ${layer}`);
    }
    const autowired = annotation('Autowired');
    if (autowired) add(violations, 'A09', p, lineOf(code, autowired.index), 'field/setter injection: use constructor injection');

    if (feature && featuresWithDomain.has(feature) && !(layer === 'infrastructure' && sub === 'persistence')) {
      const m = annotation('Entity');
      if (m && layer !== 'domain' && layer !== 'application' && layer !== 'api') add(violations, 'A11', p, lineOf(code, m.index), `@Entity of a rich feature must live in infrastructure.persistence (feature '${feature}')`);
    }
  }

  const crowded = new Map();
  for (const p of parsed) {
    const { feature } = describe(p);
    if (!feature || legacySet.has(feature) || !p.pkg.startsWith(base)) continue;
    crowded.set(p.pkg, (crowded.get(p.pkg) ?? 0) + 1);
  }
  for (const [pkg, count] of crowded) {
    if (count > MAX_FLAT_PACKAGE) warnings.push({ rule: 'W04', file: pkg.replaceAll('.', '/'), line: 0, message: `${count} classes in one package: group by sub-domain (not by technical type) once it is hard to read at a glance` });
  }

  if (strict && legacyFiles.length > 0) {
    violations.push({ rule: 'LEGACY', file: '-', line: 0, message: `${legacyFiles.length} file(s) still in legacy packages: ${[...legacySet].join(', ')}` });
  }
  violations.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line || a.rule.localeCompare(b.rule));
  return { files: parsed.length, violations, warnings, legacy: { packages: [...legacySet], files: legacyFiles } };
}

function main() {
  let options;
  try {
    options = parseArgs(process.argv.slice(2));
  } catch (error) {
    console.error(`error: ${error.message}\nusage: check-architecture.mjs --src <src/main/java> --base-package <pkg> [--legacy a,b,c] [--strict] [--json]`);
    process.exit(2);
  }
  let result;
  try {
    result = check(options);
  } catch (error) {
    console.error(`error: ${error.message}`);
    process.exit(2);
  }
  if (options.json) {
    console.log(JSON.stringify(result, null, 2));
  } else {
    for (const v of result.violations) console.log(`${v.file}:${v.line}  ${v.rule}  ${v.message}`);
    for (const w of result.warnings) console.log(`${w.file}:${w.line}  ${w.rule} (warning)  ${w.message}`);
    console.log(`\nchecked ${result.files} files: ${result.violations.length} violation(s), ${result.warnings.length} warning(s)` +
      (result.legacy.files.length ? `, ${result.legacy.files.length} file(s) still in legacy packages (${result.legacy.packages.join(', ')})` : ''));
  }
  process.exit(result.violations.length > 0 ? 1 : 0);
}

if (process.argv[1]?.endsWith('check-architecture.mjs')) main();
