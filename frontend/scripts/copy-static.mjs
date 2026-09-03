// 一体交付：Vite dist → Spring Boot static，提交进 git 后评审机无 node 也 mvn spring-boot:run 整站演示
import { cpSync, rmSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const frontend = join(here, '..');
const dist = join(frontend, 'dist');
const target = join(frontend, '..', 'multiverse-engine', 'src', 'main', 'resources', 'static');

mkdirSync(target, { recursive: true });
rmSync(target, { recursive: true, force: true });
cpSync(dist, target, { recursive: true });
console.log(`[copy-static] ${dist} → ${target}`);
