async function main() {
  const listRes = await fetch('http://localhost:9222/json');
  const pages = await listRes.json();
  console.log('Pages count:', pages.length);
  if (!pages.length) return;
  const wsUrl = pages[0].webSocketDebuggerUrl;
  console.log('Connecting to:', wsUrl);

  const ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    let id = 1;
    function send(method, params = {}) {
      ws.send(JSON.stringify({ id: id++, method, params }));
    }

    send('Runtime.enable');
    send('Runtime.evaluate', {
      expression: `(() => {
        const app = document.getElementById('app');
        const canvas = document.querySelector('canvas');
        return {
          appSize: app ? { w: app.clientWidth, h: app.clientHeight } : null,
          canvasSize: canvas ? { w: canvas.width, h: canvas.height, styleW: canvas.style.width, styleH: canvas.style.height } : null,
          hasCompanion: !!window.companion,
          bodyHtml: document.body.innerHTML.substring(0, 300)
        };
      })()`,
      returnByValue: true
    });
  };

  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    console.log('WS Response:', JSON.stringify(msg, null, 2));
    if (msg.result && msg.result.result) {
      setTimeout(() => process.exit(0), 100);
    }
  };

  ws.onerror = (err) => console.error('WS Error:', err);
}

main().catch(console.error);
