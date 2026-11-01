const { chromium } = require('playwright'); const fs = require('fs'); const path=require('path');
const D = __dirname;
const ff = `@font-face{font-family:Rye;src:url(data:font/woff2;base64,${fs.readFileSync(D+'/fontsource-rye-5.3.0/package/files/rye-latin-400-normal.woff2').toString('base64')})}
@font-face{font-family:'Special Elite';src:url(data:font/woff2;base64,${fs.readFileSync(D+'/fontsource-special-elite-5.3.0/package/files/special-elite-latin-400-normal.woff2').toString('base64')})}
html,body{margin:0;background:transparent}svg{display:block}`;
const jobs = [ // name, svg w,h, output w
 ['emblem',512,512,[400,512,1024]], ['emblem_named',512,512,[400,512,1024]],
 ['banner',1920,480,[1920]], ['ingame',700,220,[700]] ];
(async()=>{ const b = await chromium.launch(); fs.mkdirSync(D+'/out',{recursive:true});
 for (const [n,w,h,outs] of jobs) for (const ow of outs) {
  const sf = ow/w; const p = await b.newPage({viewport:{width:w,height:h},deviceScaleFactor:sf});
  const svg = fs.readFileSync(`${D}/${n}.svg`,'utf8');
  await p.setContent(`<style>${ff}</style>${svg}`); await p.evaluate(()=>document.fonts.ready);
  await p.waitForTimeout(200); if(n==='banner'){console.log(await p.evaluate(()=>[...document.querySelectorAll('text')].map(t=>{const r=t.getBBox();return t.textContent+':'+Math.round(r.x+r.width)})))}
  const suffix = outs.length>1?`_${ow}`:'';
  await p.screenshot({path:`${D}/out/${n}${suffix}.png`, omitBackground:true}); await p.close(); }
 await b.close(); })();
