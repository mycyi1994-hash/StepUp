import fs from 'node:fs/promises';
const sourceUrl = new URL('./preview.html', import.meta.url);
const outputUrl = new URL('../reference-fragment.html', import.meta.url);
let fragment = await fs.readFile(sourceUrl, 'utf8');
for (const [tag, relativePath, mime] of [
  ['__LOGO__', '../assets/logo.png', 'image/png'],
  ['__SHOE__', '../assets/shoe_1201.webp', 'image/webp']
]) {
  const bytes = await fs.readFile(new URL(relativePath, import.meta.url));
  fragment = fragment.replaceAll(tag, 'data:' + mime + ';base64,' + bytes.toString('base64'));
}
if (Buffer.byteLength(fragment) > 1000000) throw new Error('Fragment exceeds 1 MB');
if (/__(LOGO|SHOE)__/.test(fragment)) throw new Error('Unresolved image placeholder');
if (/<!doctype|<html|<head>|<body>/i.test(fragment)) throw new Error('Expected an HTML fragment');
const script = fragment.match(/<script>([\s\S]*?)<\/script>/)?.[1];
if (!script) throw new Error('Missing script');
new Function(script);
await fs.writeFile(outputUrl, fragment);
console.log(JSON.stringify({output: outputUrl.pathname, bytes: Buffer.byteLength(fragment), scriptSyntax: 'pass'}));
