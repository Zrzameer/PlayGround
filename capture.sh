#!/usr/bin/env bash
# Capture desktop + mobile screenshots of a preview URL with an isolated browser.
# Env: CAPTURE_URL (exact URL to open), CAPTURE_DIR (output dir for
# final-desktop.png / final-mobile.png). Leaves the app running; capture
# output stays outside the project source. Exit 75 = temporary infra failure,
# exit 1 = script or rendering defect.
set -euo pipefail

cd "$(dirname "$0")" # shell builtin: not timeable via /usr/bin/time
/usr/bin/time -p pwd
if [[ -z "${CAPTURE_URL:-}" || -z "${CAPTURE_DIR:-}" ]]; then
  echo "Set CAPTURE_URL and CAPTURE_DIR." >&2
  exit 1
fi
/usr/bin/time -p mkdir -p "$CAPTURE_DIR"

RUN_TMP="${RUNNER_TEMP:-/home/runner/work/_temp}"
/usr/bin/time -p mkdir -p "$RUN_TMP"
HELPER="$RUN_TMP/capture-helper.mjs"
/usr/bin/time -p tee "$HELPER" > /dev/null <<'NODE_EOF'
import { createRequire } from 'node:module';
import { mkdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
const runtime = join(process.env.HOME || '/home/runner', '.local/share/omgithub-playwright');
const require = createRequire(join(runtime, 'package.json'));
const { chromium } = require('playwright');
const url = process.env.CAPTURE_URL, output = process.env.CAPTURE_DIR;
if (!url || !output) { console.error('Set CAPTURE_URL and CAPTURE_DIR.'); process.exitCode = 1; }
else {
  mkdirSync(output, { recursive: true });
  const config = JSON.parse(readFileSync(join(runtime, process.platform === 'darwin' ? 'metal.json' : 'linux.json'), 'utf8'));
  if (process.platform === 'linux') process.env.DISPLAY ||= ':' + readFileSync(join(runtime, 'display'), 'utf8').trim();
  const transient = (error) => { throw Object.assign(error instanceof Error ? error : new Error(String(error)), { exitCode: 75 }); };
  let browser;
  try {
    browser = await chromium.launch({ ...config.browser.launchOptions, timeout: 30000 }).catch(transient);
    for (const [name, width, height] of [['desktop', 1440, 900], ['mobile', 390, 844]]) {
      const page = await browser.newPage({ viewport: { width, height } }).catch(transient);
      page.setDefaultTimeout(30000);
      page.on('pageerror', (error) => console.error('pageerror:', error.message));
      const response = await page.goto(url, { waitUntil: 'load', timeout: 45000 }).catch(transient);
      if (!response?.ok()) {
        const s = response?.status();
        throw Object.assign(new Error(`HTTP ${s} loading preview`), { exitCode: !response || [408, 429, 500, 502, 503, 504].includes(s) ? 75 : 1 });
      }
      await page.locator('body').waitFor({ state: 'visible' });
      await page.waitForFunction(() => document.fonts.status === 'loaded');
      await page.waitForTimeout(1200);
      await page.screenshot({ path: join(output, `final-${name}.png`), timeout: 30000 }).catch((error) => {
        if (error.name === 'TimeoutError' || !browser.isConnected()) transient(error);
        throw error;
      });
      console.log(`Captured final-${name}.png`);
      await page.close();
    }
  } catch (error) { console.error(error?.message || error); process.exitCode = error?.exitCode || 1; }
  finally { await browser?.close().catch((error) => { console.error(error?.message || error); process.exitCode ||= 75; }); }
}
NODE_EOF

set +e
/usr/bin/time -p node "$HELPER"
code=$?
set -e
/usr/bin/time -p test -f "$CAPTURE_DIR/final-desktop.png"
/usr/bin/time -p test -f "$CAPTURE_DIR/final-mobile.png"
echo "Capture done in $CAPTURE_DIR (exit $code)."
exit "$code"
