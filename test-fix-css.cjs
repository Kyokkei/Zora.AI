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
        document.documentElement.style.height = '100vh';
        document.body.style.height = '100vh';
        const app = document.getElementById('app');
        app.style.position = 'fixed';
        app.style.inset = '0';
        app.style.width = '100vw';
        app.style.height = '100vh';
        
        // Trigger resize
        window.dispatchEvent(new Event('resize'));
        return {
          windowInner: { w: window.innerWidth, h: window.innerHeight },
          appClient: { w: app.clientWidth, h: app.clientHeight },
          canvas: { w: document.querySelector('canvas')?.width, h: document.querySelector('canvas')?.height }
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
}

main().catch(console.error);
