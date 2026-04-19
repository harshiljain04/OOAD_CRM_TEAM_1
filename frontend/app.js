const toast = document.getElementById("toast");

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.classList.remove("hidden", "error");
  if (isError) {
    toast.classList.add("error");
  }
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    toast.classList.add("hidden");
  }, 3200);
}

function encodeForm(data) {
  return new URLSearchParams(data).toString();
}

async function request(path, options = {}) {
  const config = {
    method: options.method || "GET",
    headers: options.headers || {},
    body: options.body || null
  };

  const response = await fetch(path, config);
  const payload = await response.json().catch(() => ({ ok: false, error: "Invalid server response" }));

  if (!response.ok || !payload.ok) {
    throw new Error(payload.error || `Request failed with status ${response.status}`);
  }

  return payload;
}

function output(id, data) {
  const box = document.getElementById(id);
  box.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

function formDataObject(form) {
  const raw = Object.fromEntries(new FormData(form).entries());
  Object.keys(raw).forEach((key) => {
    if (typeof raw[key] === "string") {
      raw[key] = raw[key].trim();
    }
    if (raw[key] === "") {
      delete raw[key];
    }
  });
  return raw;
}

async function submitForm(formId, endpoint, outputId, successMsg, method = "POST") {
  const form = document.getElementById(formId);
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      const data = formDataObject(form);
      const url = method === "GET" ? `${endpoint}?${encodeForm(data)}` : endpoint;
      const payload = await request(url, {
        method,
        headers: method === "POST" ? { "Content-Type": "application/x-www-form-urlencoded" } : {},
        body: method === "POST" ? encodeForm(data) : null
      });
      output(outputId, payload);
      showToast(successMsg);
    } catch (err) {
      showToast(err.message, true);
      output(outputId, { ok: false, error: err.message });
    }
  });
}

submitForm("lead-create-form", "/api/leads/create", "lead-output", "Lead created");
submitForm("lead-status-form", "/api/leads/status", "lead-output", "Lead status fetched", "GET");
submitForm("lead-advance-form", "/api/leads/advance", "lead-output", "Lead advanced");

submitForm("customer-create-form", "/api/customers/create", "customer-output", "Customer created");
submitForm("customer-update-form", "/api/customers/update", "customer-output", "Customer updated");
submitForm("customer-sync-form", "/api/customers/sync", "customer-output", "ERP sync complete");
submitForm("customer-delete-form", "/api/customers/delete", "customer-output", "Customer deleted");

submitForm("interaction-log-form", "/api/interactions/log", "interaction-output", "Interaction logged");

submitForm("campaign-create-form", "/api/campaigns/create", "marketing-output", "Campaign created");
submitForm("campaign-revenue-form", "/api/campaigns/revenue", "marketing-output", "Revenue recorded");
submitForm("campaign-status-form", "/api/campaigns/status", "marketing-output", "Campaign status updated");

async function loadData(buttonId, endpoint, outputId, successMessage) {
  document.getElementById(buttonId).addEventListener("click", async () => {
    try {
      const payload = await request(endpoint, { method: "GET" });
      output(outputId, payload);
      showToast(successMessage);
    } catch (err) {
      showToast(err.message, true);
      output(outputId, { ok: false, error: err.message });
    }
  });
}

loadData("customer-list-btn", "/api/customers/list", "customer-output", "Customers loaded");
loadData("interaction-list-btn", "/api/interactions/list", "interaction-output", "Interactions loaded");
loadData("campaign-list-btn", "/api/campaigns/list", "marketing-output", "Campaigns loaded");
loadData("analytics-btn", "/api/analytics/summary", "marketing-output", "Analytics loaded");

request("/api/health")
  .then(() => showToast("Frontend connected to CRM API"))
  .catch((err) => showToast(err.message, true));
