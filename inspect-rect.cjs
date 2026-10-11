async function main() {
  const listRes = await fetch('http://localhost:9222/json');
  const pages = await listRes.json();
  const ws = new WebSocket(pages[0].webSocketDebuggerUrl);

  ws.onopen = () => {
    let id = 1;
    function send(method, params = {}) {
      ws.send(JSON.stringify({ id: id++, method, params }));
    }

    send('Runtime.enable');
    send('Runtime.evaluate', {
      expression: `(() => {
        const c = document.querySelector('canvas');
        const r = c ? c.getBoundingClientRect() : null;
        return {
          rect: r ? { top: r.top, left: r.left, width: r.width, height: r.height } : null,
          innerWidth: window.innerWidth,
          innerHeight: window.innerHeight,
          devicePixelRatio: window.devicePixelRatio,
          canvasWidth: c ? c.width : null,
          canvasHeight: c ? c.height : null,
          hasScene: !!window._scene || !!window.companion
        };
      })()`,
      returnByValue: true
    });
  };

  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    if (msg.result && msg.result.result) {
      console.log('Result:', JSON.stringify(msg.result.result, null, 2));
      process.exit(0);
    }
  };
}

main().catch(console.error);
