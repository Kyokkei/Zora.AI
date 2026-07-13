const health = document.querySelector("#health");
const result = document.querySelector("#result");

fetch("/api/v1/health")
  .then((response) => response.ok ? response.json() : Promise.reject())
  .then(() => { health.textContent = "API online"; health.classList.add("online"); })
  .catch(() => { health.textContent = "API unavailable"; });

const adminKey = () => document.querySelector("#adminKey").value;

async function requestStorage(enabled) {
  result.textContent = enabled == null ? "Checking..." : "Updating...";
  const response = await fetch("/api/v1/admin/storage".replace("storage", enabled == null ? "storage" : "uploads"), {
    method: enabled == null ? "GET" : "POST",
    headers: {
      "content-type": "application/json",
      "x-admin-key": adminKey()
    },
    body: enabled == null ? undefined : JSON.stringify({ enabled })
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    result.textContent = body.error || "Request failed";
    return;
  }
  const used = (body.storageBytes / 1_000_000).toFixed(2);
  const limit = (body.hardLimitBytes / 1_000_000_000).toFixed(1);
  result.textContent = `${body.uploadsEnabled ? "Uploads enabled" : "Uploads paused"} | ${used} MB of ${limit} GB | ${body.pendingCount} pending`;
}

document.querySelector("#refreshStorage").addEventListener("click", () => requestStorage(null));
document.querySelector("#enableUploads").addEventListener("click", () => requestStorage(true));
document.querySelector("#disableUploads").addEventListener("click", () => requestStorage(false));
