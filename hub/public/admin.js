const health = document.querySelector("#health");
const result = document.querySelector("#result");

fetch("/api/v1/health")
  .then((response) => response.ok ? response.json() : Promise.reject())
  .then(() => { health.textContent = "API online"; health.classList.add("online"); })
  .catch(() => { health.textContent = "API unavailable"; });

document.querySelector("#createInvite").addEventListener("click", async () => {
  result.textContent = "Creating...";
  const response = await fetch("/api/v1/admin/invites", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-admin-key": document.querySelector("#adminKey").value
    },
    body: JSON.stringify({ expiresInDays: Number(document.querySelector("#days").value) })
  });
  const body = await response.json().catch(() => ({}));
  result.textContent = response.ok ? `Invite: ${body.code}` : (body.error || "Request failed");
});
