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
        const b = document.body;
        const html = document.documentElement;
        const app = document.getElementById('app');

        const cs = (el) => {
          if (!el) return null;
          const s = window.getComputedStyle(el);
          return {
            display: s.display,
            position: s.position,
            width: s.width,
            height: s.height,
            overflow: s.overflow,
            visibility: s.visibility,
            opacity: s.opacity
          };
        };

        return {
          html: cs(html),
          body: cs(b),
          app: cs(app),
          canvas: cs(c),
          windowInner: { w: window.innerWidth, h: window.innerHeight },
          screen: { w: screen.width, h: screen.height }
        };
      })()`,
      returnByValue: true
    });
  };

  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    if (msg.result && msg.result.result) {
      console.log('Computed Styles:', JSON.stringify(msg.result.result, null, 2));
      process.exit(0);
    }
  };
}

main().catch(console.error);
