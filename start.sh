#!/usr/bin/env bash
# Personal Agent web preview: serve PROJECT_DIR/dist in the foreground.
# Writes worker metadata (deployment-output.json) to OPENCODE_WEB_DIR only;
# source and built output stay inside PROJECT_DIR.
set -euo pipefail

cd "$(dirname "$0")" # shell builtin: not timeable via /usr/bin/time
/usr/bin/time -p pwd
/usr/bin/time -p test -f dist/index.html
PORT="${PORT:-3000}"
/usr/bin/time -p test "$PORT" -ge 1 -a "$PORT" -le 65535 2>/dev/null || { echo "PORT must be 1-65535." >&2; exit 1; }

/usr/bin/time -p mkdir -p dist
APK_SRC="personal-agent/app/build/outputs/apk/debug/app-debug.apk"
if /usr/bin/time -p test -f "$APK_SRC"; then
  if /usr/bin/time -p test dist/personal-agent-debug.apk -nt "$APK_SRC" 2>/dev/null; then
    echo "APK preview copy is up to date."
  else
    /usr/bin/time -p cp -f "$APK_SRC" dist/personal-agent-debug.apk
    echo "Staged APK preview copy."
  fi
else
  echo "Warning: $APK_SRC not built yet; preview page served without APK download." >&2
fi

WEB_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"
export WEB_DIR PORT
/usr/bin/time -p mkdir -p "$WEB_DIR"
/usr/bin/time -p node -e 'const fs=require("fs"),path=require("path");fs.writeFileSync(path.join(process.env.WEB_DIR,"deployment-output.json"),JSON.stringify({project:process.cwd(),directory:path.join(process.cwd(),"dist")}))'
echo "Wrote $WEB_DIR/deployment-output.json"

/usr/bin/time -p node -e '
const http=require("http"),fs=require("fs"),path=require("path");
const root=path.join(process.cwd(),"dist");
const mime={".html":"text/html",".js":"application/javascript",".css":"text/css",".json":"application/json",".svg":"image/svg+xml",".png":"image/png",".jpg":"image/jpeg",".webp":"image/webp",".apk":"application/vnd.android.package-archive"};
const server=http.createServer((req,res)=>{
  try{
    if(req.method!=="GET"&&req.method!=="HEAD"){res.writeHead(405);res.end();return;}
    const url=new URL(req.url,"http://localhost");
    let p=path.normalize(path.join(root,decodeURIComponent(url.pathname)));
    if(p!==root&&!p.startsWith(root+path.sep)){res.writeHead(404);res.end("Not found");return;}
    if(fs.existsSync(p)&&fs.statSync(p).isDirectory())p=path.join(p,"index.html");
    if(!fs.existsSync(p))p=path.join(root,"index.html");
    res.setHeader("Content-Type",mime[path.extname(p)]||"application/octet-stream");
    res.setHeader("Cache-Control","no-cache");
    if(req.method==="HEAD"){res.end();return;}
    fs.createReadStream(p).on("error",()=>{res.writeHead(404);res.end("Not found");}).pipe(res);
  }catch{res.writeHead(404);res.end("Not found");}
});
server.listen(Number(process.env.PORT||"3000"),"0.0.0.0",()=>console.log("Serving "+root+" on port "+(process.env.PORT||"3000")));
'
