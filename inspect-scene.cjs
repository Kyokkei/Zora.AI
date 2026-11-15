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
        // We can inspect Three.js scene from the global scope or walk the DOM / modules
        // In main.ts, let's see what is in window.companion
        const keys = Object.keys(window.companion || {});
        
        // Let's check canvas WebGL draw calls or errors
        const canvas = document.querySelector('canvas');
        return {
          companionKeys: keys,
          canvasStyle: canvas ? canvas.style.cssText : '',
          canvasRect: canvas ? canvas.getBoundingClientRect() : null,
          title: document.title,
          location: window.location.href
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
