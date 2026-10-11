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
        // Fix resize in main.ts
        const w = window.innerWidth;
        const h = window.innerHeight;
        const canvas = document.querySelector('canvas');
        canvas.style.width = '100%';
        canvas.style.height = '100%';
        
        // Find three.js renderer
        // In companion, we can call resize with innerWidth / innerHeight
        window.companion.resize = function() {
          const width = window.innerWidth;
          const height = window.innerHeight;
          // canvas
          canvas.width = width * (window.devicePixelRatio || 2);
          canvas.height = height * (window.devicePixelRatio || 2);
        };
        window.companion.resize();
        
        return {
          w, h,
          canvasW: canvas.width,
          canvasH: canvas.height,
          canvasStyle: canvas.style.cssText
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
