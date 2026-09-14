const store = {
  get baseUrl() { return localStorage.getItem('ac.apiBaseUrl') || 'http://localhost:8080'; },
  set baseUrl(value) { localStorage.setItem('ac.apiBaseUrl', value.replace(/\/$/, '')); },
  get token() { return sessionStorage.getItem('ac.apiToken') || ''; },
  set token(value) { sessionStorage.setItem('ac.apiToken', value.trim()); }
};

const state = {
  profile: null,
  vendors: [],
  selectedVendor: null,
  selectedLocation: null,
  commerce: null,
  dasher: null,
  zones: [],
  currentView: 'overview',
  lastActivity: null
};

const views = {
  overview: ['Marketplace workspace', 'Overview'],
  customer: ['Customer workspace', 'Profile & preferences'],
  vendor: ['Merchant workspace', 'Vendor studio'],
  dasher: ['Courier workspace', 'Dasher command'],
  operator: ['Operations workspace', 'Mission control'],
  rest: ['Developer workspace', 'REST Lab']
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

class ApiError extends Error {
  constructor(status, data, elapsed) {
    super(data?.detail || data?.message || `Request failed with status ${status}`);
    this.status = status;
    this.data = data;
    this.elapsed = elapsed;
  }
}

function authHeaders() {
  return store.token ? { Authorization: `Bearer ${store.token}` } : {};
}

async function api(path, { method = 'GET', body, authenticated = true } = {}) {
  const started = performance.now();
  const headers = { Accept: 'application/json' };
  if (authenticated) Object.assign(headers, authHeaders());
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(`${store.baseUrl}${path.startsWith('/') ? path : `/${path}`}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    });
  } catch (error) {
    updateConnection(false, 'Unavailable');
    recordActivity(method, path, 0, { detail: error.message }, performance.now() - started);
    throw new ApiError(0, { detail: `Could not reach ${store.baseUrl}` }, performance.now() - started);
  }
  const elapsed = Math.round(performance.now() - started);
  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }
  recordActivity(method, path, response.status, data, elapsed);
  if (!response.ok) throw new ApiError(response.status, data, elapsed);
  return { data, status: response.status, elapsed };
}

function recordActivity(method, path, status, data, elapsed) {
  state.lastActivity = { method, path, status, data, elapsed };
  const target = $('#recent-activity');
  if (!target) return;
  target.className = 'activity-card';
  target.innerHTML = '';
  const headline = document.createElement('div');
  headline.className = 'activity-head';
  const code = document.createElement('span');
  code.className = `response-code ${status >= 400 || status === 0 ? 'error' : ''}`;
  code.textContent = status || 'NETWORK';
  const route = document.createElement('strong');
  route.textContent = `${method} ${path}`;
  const timing = document.createElement('small');
  timing.textContent = `${Math.round(elapsed)} ms`;
  headline.append(code, route, timing);
  const preview = document.createElement('pre');
  preview.textContent = pretty(data).slice(0, 700);
  target.append(headline, preview);
}

function pretty(value) {
  if (value === null || value === undefined || value === '') return 'No response body';
  return typeof value === 'string' ? value : JSON.stringify(value, null, 2);
}

function notify(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast ${type === 'error' ? 'error' : ''}`;
  toast.textContent = message;
  $('#toast-region').append(toast);
  setTimeout(() => toast.remove(), 4200);
}

function report(error) {
  console.error(error);
  notify(error.message || 'Something went wrong', 'error');
}

function updateConnection(ok, label = ok ? 'Healthy' : 'Unavailable') {
  $('#connection-dot').className = `status-dot ${ok ? 'ok' : 'bad'}`;
  $('#connection-label').textContent = label;
  $('#overview-health').textContent = label;
  $('#overview-api-url').textContent = store.baseUrl;
}

function decodeToken() {
  if (!store.token) return null;
  try {
    const payload = store.token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(decodeURIComponent(atob(payload).split('').map(char =>
      `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`
    ).join('')));
  } catch { return null; }
}

function updateSessionDisplay() {
  const claims = decodeToken();
  $('#session-name').textContent = claims?.name || claims?.email || 'API session';
  $('#session-subject').textContent = claims?.sub || (store.token ? 'Token configured' : 'JWT required');
  const initials = (claims?.name || claims?.email || 'API').split(/\s|@/).filter(Boolean).slice(0, 2).map(value => value[0]).join('').toUpperCase();
  $('.avatar').textContent = initials || 'API';
  $('#api-base-url').value = store.baseUrl;
  $('#api-token').value = store.token;
}

async function checkHealth() {
  try {
    const { data } = await api('/actuator/health', { authenticated: false });
    updateConnection(data?.status === 'UP', data?.status === 'UP' ? 'Healthy' : data?.status || 'Degraded');
  } catch { updateConnection(false); }
}

function showView(name) {
  if (!views[name]) return;
  state.currentView = name;
  $$('.view').forEach(view => view.classList.toggle('active', view.id === `view-${name}`));
  $$('.nav-item').forEach(item => item.classList.toggle('active', item.dataset.view === name));
  $('#page-kicker').textContent = views[name][0];
  $('#page-title').textContent = views[name][1];
  history.replaceState(null, '', `#${name}`);
}

function csv(value) {
  return value.split(',').map(item => item.trim()).filter(Boolean);
}

function numberOrNull(value) {
  return value === '' ? null : Number(value);
}

function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function loadProfile({ quiet = false } = {}) {
  try {
    const { data } = await api('/v1/me');
    state.profile = data;
    const form = $('#customer-form');
    for (const key of ['displayName', 'timezone', 'locale', 'defaultTipPercentage', 'autoOrderMaxAmountMinor']) {
      if (form.elements[key]) form.elements[key].value = data[key] ?? '';
    }
    form.elements.dietaryRestrictions.value = (data.dietaryRestrictions || []).join(', ');
    form.elements.allergens.value = (data.allergens || []).join(', ');
    form.elements.substitutionMode.value = data.substitutionMode;
    form.elements.autoOrderEnabled.checked = data.autoOrderEnabled;
    $('#metric-identity').textContent = data.displayName;
    $('#metric-roles').textContent = (data.roles || []).join(' · ');
    if (!quiet) notify('Customer profile loaded');
    return data;
  } catch (error) { if (!quiet) report(error); throw error; }
}

async function saveProfile(event) {
  event.preventDefault();
  try {
    if (!state.profile) await loadProfile({ quiet: true });
    const form = event.currentTarget;
    const values = formValues(form);
    const body = {
      displayName: values.displayName,
      timezone: values.timezone,
      locale: values.locale,
      defaultTipPercentage: Number(values.defaultTipPercentage),
      dietaryRestrictions: csv(values.dietaryRestrictions),
      allergens: csv(values.allergens),
      substitutionMode: values.substitutionMode,
      autoOrderEnabled: form.elements.autoOrderEnabled.checked,
      autoOrderMaxAmountMinor: numberOrNull(values.autoOrderMaxAmountMinor),
      expectedVersion: state.profile.version
    };
    const { data } = await api('/v1/me', { method: 'PATCH', body });
    state.profile = data;
    notify(`Profile saved at version ${data.version}`);
  } catch (error) { report(error); }
}

async function loadAddresses() {
  try {
    const { data } = await api('/v1/me/addresses');
    const list = $('#address-list');
    list.innerHTML = '';
    if (!data.length) { list.textContent = 'No addresses saved yet.'; return; }
    data.forEach(address => {
      const item = document.createElement('div');
      item.className = 'list-item';
      item.innerHTML = `<div><strong>${escapeHtml(address.label)} ${address.isDefault ? '<span class="mini-tag">Default</span>' : ''}</strong><small>${escapeHtml(address.formattedAddress)}</small></div>`;
      const remove = document.createElement('button');
      remove.className = 'button subtle'; remove.textContent = 'Delete';
      remove.addEventListener('click', async () => {
        try { await api(`/v1/me/addresses/${address.id}`, { method: 'DELETE' }); await loadAddresses(); notify('Address removed'); } catch (error) { report(error); }
      });
      item.append(remove); list.append(item);
    });
  } catch (error) { report(error); }
}

async function createAddress(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const values = formValues(form);
  try {
    await api('/v1/me/addresses', { method: 'POST', body: {
      ...values,
      latitude: Number(values.latitude), longitude: Number(values.longitude),
      isDefault: form.elements.isDefault.checked
    }});
    await loadAddresses(); notify('Address added');
  } catch (error) { report(error); }
}

async function setConsent(button) {
  try {
    await api(`/v1/me/consents/${button.dataset.type}`, { method: 'POST', body: {
      granted: button.dataset.granted === 'true', policyVersion: $('#policy-version').value
    }});
    notify(`${button.dataset.type.replaceAll('_', ' ')} updated`);
  } catch (error) { report(error); }
}

async function loadVendors({ quiet = false } = {}) {
  try {
    const { data } = await api('/v1/vendors/me');
    state.vendors = data;
    $('#vendor-count').textContent = data.length;
    $('#metric-vendors').textContent = String(data.length).padStart(2, '0');
    const list = $('#vendor-list'); list.innerHTML = '';
    if (!data.length) { list.textContent = 'No vendors yet. Create your first draft.'; return data; }
    data.forEach(vendor => {
      const item = document.createElement('div'); item.className = 'list-item';
      item.innerHTML = `<div><strong>${escapeHtml(vendor.displayName)}</strong><small>${escapeHtml(vendor.category)} · ${escapeHtml(vendor.status)}</small></div>`;
      const select = document.createElement('button'); select.className = 'button secondary'; select.textContent = 'Select';
      select.addEventListener('click', () => selectVendor(vendor)); item.append(select); list.append(item);
    });
    if (!quiet) notify('Vendor portfolio loaded');
    return data;
  } catch (error) { if (!quiet) report(error); throw error; }
}

function selectVendor(vendor) {
  state.selectedVendor = vendor;
  $('#location-vendor-id').value = vendor.id;
  notify(`${vendor.displayName} selected`);
}

async function createVendor(event) {
  event.preventDefault();
  const values = formValues(event.currentTarget);
  try {
    const { data } = await api('/v1/vendors', { method: 'POST', body: {
      legalName: values.legalName, displayName: values.displayName, slug: values.slug,
      category: values.category, tags: csv(values.tags), supportEmail: values.supportEmail,
      defaultCurrency: values.defaultCurrency
    }});
    selectVendor(data); await loadVendors({ quiet: true }); notify('Draft vendor created');
  } catch (error) { report(error); }
}

async function createLocation(event) {
  event.preventDefault();
  const values = formValues(event.currentTarget);
  const vendorId = values.vendorId;
  delete values.vendorId;
  try {
    const { data } = await api(`/v1/vendors/${vendorId}/locations`, { method: 'POST', body: {
      ...values, latitude: Number(values.latitude), longitude: Number(values.longitude)
    }});
    state.selectedLocation = data; $('#commerce-location-id').value = data.id;
    notify('Draft location created; commerce settings are ready');
    await loadCommerce();
  } catch (error) { report(error); }
}

async function loadCommerce() {
  const locationId = $('#commerce-location-id').value.trim();
  if (!locationId) return notify('Enter or create a location ID', 'error');
  try {
    const { data } = await api(`/v1/vendor-locations/${locationId}/commerce-settings`);
    state.commerce = data;
    const form = $('#commerce-form');
    form.elements.isAcceptingOrders.checked = data.isAcceptingOrders;
    for (const key of ['defaultPreparationMinutes', 'minimumPreparationMinutes', 'maximumPreparationMinutes']) form.elements[key].value = data[key] ?? '';
    $('#commerce-version').textContent = data.version;
    notify('Commerce settings loaded');
  } catch (error) { report(error); }
}

async function saveCommerce(event) {
  event.preventDefault();
  try {
    if (!state.commerce) await loadCommerce();
    if (!state.commerce) return;
    const form = event.currentTarget; const values = formValues(form);
    await api(`/v1/vendor-locations/${values.locationId}/commerce-settings`, { method: 'PATCH', body: {
      isAcceptingOrders: form.elements.isAcceptingOrders.checked,
      defaultPreparationMinutes: Number(values.defaultPreparationMinutes),
      minimumPreparationMinutes: Number(values.minimumPreparationMinutes),
      maximumPreparationMinutes: Number(values.maximumPreparationMinutes),
      expectedVersion: state.commerce.version
    }});
    await loadCommerce(); notify('Commerce settings updated');
  } catch (error) { report(error); }
}

async function loadDasher({ quiet = false } = {}) {
  try {
    const { data } = await api('/v1/dashers/me'); state.dasher = data; renderDasher();
    if (!quiet) notify('Dasher profile loaded'); return data;
  } catch (error) {
    if (error.status === 404) { state.dasher = null; renderDasher(); if (!quiet) notify('No dasher profile yet'); return null; }
    if (!quiet) report(error); throw error;
  }
}

function renderDasher() {
  const profile = state.dasher;
  $('#dasher-heading').textContent = profile ? profile.status.replaceAll('_', ' ') : 'Not onboarded';
  $('#dasher-summary').textContent = profile ? `${profile.vehicleType} · background ${profile.backgroundCheckStatus.toLowerCase()} · version ${profile.version}` : 'Load your profile or begin onboarding.';
  $('#metric-dasher').textContent = profile ? profile.status.replace('_VERIFICATION', '') : 'Not enrolled';
  $('#dasher-online-dot').className = `status-dot ${profile?.online ? 'ok' : ''}`;
  $('#dasher-online-label').textContent = profile?.online ? 'Online' : 'Offline';
  const toggle = $('#toggle-availability'); toggle.disabled = !profile; toggle.textContent = profile?.online ? 'Go offline' : 'Go online';
  $('#save-dasher').textContent = profile ? 'Update profile' : 'Start onboarding';
  if (profile) {
    const form = $('#dasher-form');
    for (const key of ['vehicleType', 'vehicleMake', 'vehicleModel', 'vehicleColor', 'licensePlate', 'maxActivePickups']) form.elements[key].value = profile[key] ?? '';
    form.elements.licenseNumber.value = '';
    form.elements.licenseNumber.placeholder = `Current ending ${profile.licenseNumberLast4 || 'not set'}`;
  }
}

async function saveDasher(event) {
  event.preventDefault();
  const form = event.currentTarget; const values = formValues(form);
  const body = {
    vehicleType: values.vehicleType, vehicleMake: values.vehicleMake || null,
    vehicleModel: values.vehicleModel || null, vehicleColor: values.vehicleColor || null,
    licensePlate: values.licensePlate || null, maxActivePickups: Number(values.maxActivePickups)
  };
  if (values.licenseNumber) body.licenseNumber = values.licenseNumber;
  try {
    if (state.dasher) { body.expectedVersion = state.dasher.version; ({ data: state.dasher } = await api('/v1/dashers/me', { method: 'PATCH', body })); }
    else { ({ data: state.dasher } = await api('/v1/dashers/me/onboarding', { method: 'POST', body })); }
    renderDasher(); notify(state.dasher.version ? 'Dasher profile updated' : 'Dasher onboarding started');
  } catch (error) { report(error); }
}

async function toggleAvailability() {
  if (!state.dasher) return;
  try {
    const { data } = await api('/v1/dashers/me/availability', { method: 'PATCH', body: {
      online: !state.dasher.online, expectedVersion: state.dasher.version
    }});
    state.dasher = data; renderDasher(); notify(data.online ? 'You are online' : 'You are offline');
  } catch (error) { report(error); }
}

async function loadZones() {
  try {
    const { data } = await api('/v1/dashers/me/operating-zones'); state.zones = data;
    const list = $('#zone-list'); list.innerHTML = '';
    if (!data.length) { list.textContent = 'No operating zones configured.'; return; }
    data.forEach(zone => {
      const item = document.createElement('div'); item.className = 'list-item';
      item.innerHTML = `<div><strong>${escapeHtml(zone.zoneName)}</strong><small>PostGIS multipolygon · ${zone.id.slice(0, 8)}</small></div>`;
      const remove = document.createElement('button'); remove.className = 'button subtle'; remove.textContent = 'Delete';
      remove.addEventListener('click', async () => { try { await api(`/v1/dashers/me/operating-zones/${zone.id}`, { method: 'DELETE' }); await loadZones(); notify('Operating zone removed'); } catch (error) { report(error); } });
      item.append(remove); list.append(item);
    });
  } catch (error) { report(error); }
}

async function createZone(event) {
  event.preventDefault(); const values = formValues(event.currentTarget);
  try {
    const boundary = values.boundary.split(/\r?\n/).filter(Boolean).map(line => {
      const [latitude, longitude] = line.split(',').map(Number); return { latitude, longitude };
    });
    await api('/v1/dashers/me/operating-zones', { method: 'POST', body: { zoneName: values.zoneName, boundary } });
    await loadZones(); notify('Operating zone added');
  } catch (error) { report(error); }
}

async function reviewDasher(event) {
  event.preventDefault(); const values = formValues(event.currentTarget);
  try {
    const { data } = await api(`/v1/operators/dashers/${values.dasherId}/verification`, { method: 'PATCH', body: {
      status: values.status, backgroundCheckStatus: values.backgroundCheckStatus,
      expectedVersion: Number(values.expectedVersion)
    }});
    notify(`Dasher is now ${data.status.toLowerCase()}`);
  } catch (error) { report(error); }
}

const presets = {
  profile: { method: 'GET', path: '/v1/me' },
  vendors: { method: 'GET', path: '/v1/vendors/me' },
  createVendor: { method: 'POST', path: '/v1/vendors', body: { legalName: 'REST Lab Foods LLC', displayName: 'REST Lab Foods', slug: `rest-lab-${Date.now().toString().slice(-7)}`, category: 'RESTAURANT', tags: ['api-test'], supportEmail: 'owner@example.com', defaultCurrency: 'USD' } },
  dasher: { method: 'GET', path: '/v1/dashers/me' },
  health: { method: 'GET', path: '/actuator/health', authenticated: false }
};

function applyPreset() {
  const preset = presets[$('#rest-preset').value]; if (!preset) return;
  $('#rest-method').value = preset.method; $('#rest-path').value = preset.path;
  $('#rest-body').value = preset.body ? pretty(preset.body) : '';
}

async function sendRest() {
  const method = $('#rest-method').value; const path = $('#rest-path').value.trim();
  const started = performance.now();
  try {
    let body;
    if (!['GET', 'DELETE'].includes(method) && $('#rest-body').value.trim()) body = JSON.parse($('#rest-body').value);
    const preset = presets[$('#rest-preset').value];
    const result = await api(path, { method, body, authenticated: preset?.authenticated !== false });
    showRestResponse(result.status, result.data, result.elapsed);
  } catch (error) {
    if (error instanceof SyntaxError) { showRestResponse(0, { detail: 'Request body is not valid JSON' }, performance.now() - started); return report(error); }
    showRestResponse(error.status, error.data, error.elapsed); report(error);
  }
}

function showRestResponse(status, data, elapsed) {
  const label = $('#response-status'); label.textContent = status || 'ERROR';
  label.className = `response-code ${status >= 400 || status === 0 ? 'error' : ''}`;
  $('#response-time').textContent = `${Math.round(elapsed || 0)} ms`;
  $('#rest-response').textContent = pretty(data);
}

function escapeHtml(value = '') {
  return String(value).replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character]);
}

async function refreshCurrentView() {
  const actions = { overview: loadOverview, customer: () => loadProfile(), vendor: () => loadVendors(), dasher: async () => { await loadDasher(); if (state.dasher) await loadZones(); }, rest: checkHealth };
  try { await (actions[state.currentView] || checkHealth)(); } catch { /* handled by action */ }
}

async function loadOverview() {
  await checkHealth();
  if (!store.token) return;
  const results = await Promise.allSettled([loadProfile({ quiet: true }), loadVendors({ quiet: true }), loadDasher({ quiet: true })]);
  if (results.some(result => result.status === 'fulfilled')) updateConnection(true, 'Connected');
}

function bindEvents() {
  $$('.nav-item').forEach(button => button.addEventListener('click', () => showView(button.dataset.view)));
  $$('[data-jump]').forEach(button => button.addEventListener('click', () => showView(button.dataset.jump)));
  $('#open-session').addEventListener('click', () => $('#session-dialog').showModal());
  $('#session-chip').addEventListener('click', () => $('#session-dialog').showModal());
  $('#save-session').addEventListener('click', async event => {
    event.preventDefault(); store.baseUrl = $('#api-base-url').value.trim(); store.token = $('#api-token').value;
    updateSessionDisplay(); $('#session-dialog').close(); await loadOverview(); notify('API session saved');
  });
  $('#refresh-view').addEventListener('click', refreshCurrentView);
  $('#load-customer').addEventListener('click', () => loadProfile());
  $('#customer-form').addEventListener('submit', saveProfile);
  $('#load-addresses').addEventListener('click', loadAddresses);
  $('#address-form').addEventListener('submit', createAddress);
  $$('.consent-button').forEach(button => button.addEventListener('click', () => setConsent(button)));
  $('#load-vendors').addEventListener('click', () => loadVendors());
  $('#vendor-form').addEventListener('submit', createVendor);
  $('#location-form').addEventListener('submit', createLocation);
  $('#load-commerce').addEventListener('click', loadCommerce);
  $('#commerce-form').addEventListener('submit', saveCommerce);
  $('#load-dasher').addEventListener('click', () => loadDasher());
  $('#dasher-form').addEventListener('submit', saveDasher);
  $('#toggle-availability').addEventListener('click', toggleAvailability);
  $('#load-zones').addEventListener('click', loadZones);
  $('#zone-form').addEventListener('submit', createZone);
  $('#operator-form').addEventListener('submit', reviewDasher);
  $('#rest-preset').addEventListener('change', applyPreset);
  $('#send-rest').addEventListener('click', sendRest);
  $('#copy-response').addEventListener('click', async () => { await navigator.clipboard.writeText($('#rest-response').textContent); notify('Response copied'); });
}

function initialize() {
  bindEvents(); updateSessionDisplay();
  const view = location.hash.slice(1); showView(views[view] ? view : 'overview');
  const slug = `neighborhood-foods-${Date.now().toString().slice(-6)}`;
  $('#vendor-form').elements.slug.value = slug;
  $('#location-form').elements.slug.value = `pine-street-${Date.now().toString().slice(-6)}`;
  loadOverview();
}

document.addEventListener('DOMContentLoaded', initialize);