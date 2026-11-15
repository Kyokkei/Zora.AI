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
        const canvas = document.querySelector('canvas');
        const gl = canvas?.getContext('webgl2') || canvas?.getContext('webgl');
        const pixels = new Uint8Array(4);
        if (gl) {
          gl.readPixels(canvas.width / 2, canvas.height / 2, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
        }
        return {
          canvasW: canvas?.width,
          canvasH: canvas?.height,
          canvasClientW: canvas?.clientWidth,
          canvasClientH: canvas?.clientHeight,
          glVendor: gl ? gl.getParameter(gl.VENDOR) : null,
          glRenderer: gl ? gl.getParameter(gl.RENDERER) : null,
          glError: gl ? gl.getError() : null,
          pixelCenter: Array.from(pixels),
          windowSize: { w: window.innerWidth, h: window.innerHeight },
          docHtml: document.body.innerHTML.substring(0, 400)
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
