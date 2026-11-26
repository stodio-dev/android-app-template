const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  const [,, dataFile, out] = process.argv;
  const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  const page = await browser.newPage({ viewport: { width: 540, height: 960 } });
  const html = fs.readFileSync(require('path').join(__dirname, 'page.html'), 'utf8').replace('<script>', '<script>window.DATA = ' + fs.readFileSync(dataFile, 'utf8') + ';</script><script>');
  page.on('console', m => { if (!/^WebGL: |GL Driver Message/.test(m.text())) console.log('console:', m.text()); });
  page.on('pageerror', e => console.log('pageerror:', e.message));
  await page.setContent(html);
  await page.waitForFunction(() => window.RESULT !== undefined, null, { timeout: 20000 });
  console.log(await page.evaluate(() => window.RESULT));
  await page.screenshot({ path: out });
  await browser.close();
})();
