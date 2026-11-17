// Shiftwise supervisor board: plain JavaScript on top of the REST API.
(() => {
  'use strict';

  const DEMO = { email: 'giulia.ferri@demo.dev', password: 'demo1234' };
  const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const $ = (id) => document.getElementById(id);

  const state = {
    token: null,
    role: null,
    areas: [],
    shifts: [],
    requirements: [],
    weekStart: null,     // Date (Monday)
    date: null,          // ISO string of the selected day
    coverage: [],
    assignments: [],
    selected: null,      // { areaId, shiftId }
    pendingPlan: false,
  };

  // ---------- helpers ----------

  const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  const parse = (s) => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); };
  const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
  const mondayOf = (d) => addDays(d, -((d.getDay() + 6) % 7));
  const fmtLong = (s) => parse(s).toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' });
  const fmtShort = (d) => d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const hm = (t) => t.slice(0, 5);
  const shiftHours = (s) => s.segments.map((g) => `${hm(g.start)}-${hm(g.end)}`).join(' + ');

  function toast(message, isError = false) {
    const el = $('toast');
    el.textContent = message;
    el.classList.toggle('error', isError);
    el.hidden = false;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => { el.hidden = true; }, isError ? 6000 : 3500);
  }

  async function api(method, path, body) {
    const res = await fetch(path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (res.status === 401 && state.token) {
      signOut();
      throw new Error('Your session expired. Please sign in again.');
    }
    if (!res.ok) {
      let msg = `${res.status} ${res.statusText}`;
      try {
        const p = await res.json();
        msg = p.violations?.length ? p.violations.map((v) => v.message).join(' · ') : (p.detail || msg);
      } catch (e) { /* not JSON */ }
      throw new Error(msg);
    }
    return res.status === 204 ? null : res.json();
  }

  async function busy(button, work) {
    if (button) button.disabled = true;
    try { await work(); } catch (e) { toast(e.message, true); } finally { if (button) button.disabled = false; }
  }

  // ---------- auth ----------

  async function signIn(email, password) {
    const r = await api('POST', '/api/auth/login', { email, password });
    if (r.role === 'EMPLOYEE') {
      throw new Error('This board is for supervisors. Use the demo supervisor account.');
    }
    state.token = r.token;
    state.role = r.role;
    try { sessionStorage.setItem('shiftwise.token', r.token); sessionStorage.setItem('shiftwise.role', r.role); } catch (e) { /* storage off */ }
    $('who').textContent = `${email} · ${r.role.toLowerCase()}`;
    await start();
  }

  function signOut() {
    state.token = null;
    try { sessionStorage.removeItem('shiftwise.token'); sessionStorage.removeItem('shiftwise.role'); } catch (e) { /* storage off */ }
    $('app-view').hidden = true;
    $('login-view').hidden = false;
  }

  // ---------- loading ----------

  async function start() {
    $('login-view').hidden = true;
    $('app-view').hidden = false;
    [state.areas, state.shifts, state.requirements] = await Promise.all([
      api('GET', '/api/areas'), api('GET', '/api/shift-templates'), api('GET', '/api/coverage-requirements'),
    ]);
    if (!state.weekStart) {
      // Start on next week's Monday: the week a supervisor is usually planning.
      state.weekStart = addDays(mondayOf(new Date()), 7);
      state.date = iso(state.weekStart);
    }
    await refresh();
  }

  async function refresh() {
    await Promise.all([loadWeek(), loadDay()]);
    renderPanel();
  }

  async function loadWeek() {
    const days = [...Array(7)].map((_, i) => addDays(state.weekStart, i));
    $('week-label').textContent = `${fmtShort(days[0])} – ${fmtShort(days[6])} ${days[6].getFullYear()}`;
    const perDay = await Promise.all(days.map((d) => api('GET', `/api/coverage?date=${iso(d)}`)));
    $('days').innerHTML = days.map((d, i) => {
      const slots = perDay[i];
      const total = slots.reduce((n, s) => n + s.required, 0) || 1;
      const filled = slots.reduce((n, s) => n + Math.min(s.assigned, s.required), 0);
      const missing = slots.reduce((n, s) => n + s.missing, 0);
      const red = slots.filter((s) => s.status === 'RED').length;
      const color = missing === 0 ? 'var(--green)' : red > 0 ? 'var(--red)' : 'var(--yellow)';
      return `<button class="day" type="button" data-date="${iso(d)}" ${iso(d) === state.date ? 'aria-current="date"' : ''}>
          <span class="dname">${DAY_NAMES[i]}</span><span class="dnum">${d.getDate()}</span>
          <span class="bar"><i style="width:${Math.round(100 * filled / total)}%;background:${color}"></i></span>
          <span class="gap">${missing === 0 ? 'covered' : `${missing} open`}</span>
        </button>`;
    }).join('');
  }

  async function loadDay() {
    [state.coverage, state.assignments] = await Promise.all([
      api('GET', `/api/coverage?date=${state.date}`),
      api('GET', `/api/assignments?from=${state.date}&to=${state.date}`),
    ]);
    renderBoard();
  }

  // ---------- board ----------

  function renderBoard() {
    $('day-title').textContent = fmtLong(state.date);
    const usedShiftIds = new Set(state.requirements.map((r) => r.shiftTemplateId));
    const shifts = state.shifts.filter((s) => usedShiftIds.has(s.id));
    const board = $('board');
    board.style.gridTemplateColumns = `minmax(150px, 1.2fr) repeat(${shifts.length}, minmax(110px, 1fr))`;

    let html = '<div></div>' + shifts.map((s) => `<div class="colhead"><b>${esc(s.code)}</b>${shiftHours(s)}</div>`).join('');
    for (const area of state.areas) {
      const onDay = state.assignments.filter((a) => a.areaCode === area.code).length;
      html += `<div class="rowhead"><b>${esc(area.name)}</b><span>${esc(area.code)} · ${onDay} on shift</span></div>`;
      for (const shift of shifts) {
        const slot = state.coverage.find((c) => c.areaCode === area.code && c.shiftCode === shift.code);
        if (!slot) {
          html += '<div class="cell none" aria-hidden="true"></div>';
          continue;
        }
        const sel = state.selected && state.selected.areaId === area.id && state.selected.shiftId === shift.id;
        const label = slot.missing === 0 ? 'covered' : `${slot.missing} short`;
        html += `<button class="cell ${slot.status}" type="button" role="gridcell" aria-selected="${sel}"
            data-area="${area.id}" data-shift="${shift.id}"
            aria-label="${esc(area.name)}, ${esc(shift.code)}: ${slot.assigned} of ${slot.required}, ${label}">
            <span class="count">${slot.assigned}<small> / ${slot.required}</small></span>
            <span class="state">${label}</span>
          </button>`;
      }
    }
    board.innerHTML = html;
  }

  // ---------- side panel ----------

  async function renderPanel() {
    const panel = $('panel');
    if (!state.selected) {
      panel.innerHTML = '<div class="panel-empty muted"><p class="mono small">// no slot selected</p><p>Pick an area and shift on the board.</p></div>';
      return;
    }
    const area = state.areas.find((a) => a.id === state.selected.areaId);
    const shift = state.shifts.find((s) => s.id === state.selected.shiftId);
    const slot = state.coverage.find((c) => c.areaCode === area.code && c.shiftCode === shift.code);
    const onShift = state.assignments.filter((a) => a.areaCode === area.code && a.shiftCode === shift.code);

    panel.innerHTML = `
      <div>
        <h3>${esc(area.name)}</h3>
        <p class="meta">${esc(shift.code)} · ${shiftHours(shift)} · ${shift.hours}h · ${slot.assigned}/${slot.required} assigned</p>
      </div>
      <div>
        <h4>On shift</h4>
        ${onShift.length ? `<ul class="people">${onShift.map((a) => `
          <li class="person"><div><div class="name">${esc(a.employeeName)}</div></div>
            <div class="acts">
              <button class="btn small" type="button" data-absent="${a.employeeId}" data-name="${esc(a.employeeName)}">Absent</button>
              <button class="btn small danger" type="button" data-remove="${a.id}" aria-label="Remove ${esc(a.employeeName)} from this shift">Remove</button>
            </div></li>`).join('')}</ul>` : '<p class="empty-note">Nobody yet.</p>'}
      </div>
      <div id="suggestions"><h4>Who can cover</h4><p class="empty-note">Checking every rule…</p></div>`;

    try {
      const s = await api('GET', `/api/suggestions?date=${state.date}&areaId=${area.id}&shiftTemplateId=${shift.id}&includeBlocked=true`);
      const ok = s.candidates.filter((c) => c.eligible);
      const blocked = s.candidates.filter((c) => !c.eligible);
      const full = slot.missing === 0;
      $('suggestions').innerHTML = `
        <h4>Who can cover${full ? ' (slot is full)' : ''}</h4>
        ${ok.length ? `<ul class="people">${ok.map((c, i) => `
          <li class="person"><div>
              <div class="name"><span class="rank">#${i + 1}</span>${esc(c.fullName)}</div>
              <div class="sub">${c.plannedHoursThisWeek}h of ${c.contractHours}h planned this week</div>
              <div class="load"><i style="width:${Math.min(100, Math.round(100 * c.plannedHoursThisWeek / c.contractHours))}%"></i></div>
            </div>
            <button class="btn small ${i === 0 && !full ? 'primary' : ''}" type="button" data-assign="${c.employeeId}" data-name="${esc(c.fullName)}">Assign</button>
          </li>`).join('')}</ul>` : '<p class="empty-note">Nobody trained for this area can take it without breaking a rule.</p>'}
        ${blocked.length ? `<details class="blocked-list"><summary>${blocked.length} trained but blocked</summary><ul class="people">${blocked.map((c) => `
          <li class="person blocked"><div><div class="name">${esc(c.fullName)}</div>
            <div class="sub">${esc(c.blockedBy.map((b) => b.message).join(' · '))}</div></div></li>`).join('')}</ul></details>` : ''}`;
    } catch (e) {
      $('suggestions').innerHTML = `<h4>Who can cover</h4><p class="empty-note">${esc(e.message)}</p>`;
    }
  }

  // ---------- actions ----------

  async function assign(button, employeeId, name) {
    await busy(button, async () => {
      await api('POST', '/api/assignments', {
        employeeId: Number(employeeId), areaId: state.selected.areaId,
        shiftTemplateId: state.selected.shiftId, date: state.date,
      });
      toast(`${name} assigned.`);
      await refresh();
    });
  }

  async function markAbsent(button, employeeId, name) {
    await busy(button, async () => {
      const r = await api('POST', '/api/absences', {
        employeeId: Number(employeeId), startDate: state.date, endDate: state.date, reason: 'Marked absent on the board',
      });
      const n = r.removedAssignments.length;
      toast(`${name} marked absent. ${n} shift${n === 1 ? '' : 's'} cancelled: pick a replacement below.`);
      await refresh();
    });
  }

  async function remove(button, assignmentId) {
    await busy(button, async () => {
      await api('DELETE', `/api/assignments/${assignmentId}`);
      toast('Removed from the shift.');
      await refresh();
    });
  }

  async function generate(dryRun, button) {
    await busy(button, async () => {
      const r = await api('POST', '/api/schedules/generate', { weekStart: iso(state.weekStart), dryRun });
      const body = $('modal-body');
      const unfilled = r.unfilled.slice(0, 12);
      body.innerHTML = `
        <p class="muted">${dryRun ? 'Preview: nothing has been saved yet.' : 'Saved. The board now shows the new shifts.'}
          Existing shifts are kept; only open slots are filled, with the same rules and fair ranking as the suggestions.</p>
        <div class="stats">
          <div class="stat"><b>${r.createdCount}</b><span class="muted small">shifts ${dryRun ? 'to add' : 'added'}</span></div>
          <div class="stat"><b>${r.unfilledCount}</b><span class="muted small">slots nobody can take</span></div>
        </div>
        ${unfilled.length ? `<div><h3 class="small muted" style="margin-bottom:6px">Still open</h3>
          <table><thead><tr><th>Day</th><th>Area</th><th>Shift</th><th>Missing</th></tr></thead><tbody>
          ${unfilled.map((u) => `<tr><td>${parse(u.date).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric' })}</td><td>${esc(u.areaCode)}</td><td>${esc(u.shiftCode)}</td><td class="num">${u.missing}</td></tr>`).join('')}
          </tbody></table>${r.unfilled.length > unfilled.length ? `<p class="small muted" style="margin-top:6px">…and ${r.unfilled.length - unfilled.length} more</p>` : ''}</div>` : ''}`;
      $('modal-apply').hidden = !dryRun || r.createdCount === 0;
      $('modal').hidden = false;
      if (!dryRun) await refresh();
    });
  }

  // ---------- events ----------

  $('demo-login').addEventListener('click', (e) => busy(e.currentTarget, () => signIn(DEMO.email, DEMO.password)));
  $('login-form').addEventListener('submit', (e) => {
    e.preventDefault();
    busy(e.submitter, () => signIn($('email').value.trim(), $('password').value));
  });
  $('logout').addEventListener('click', signOut);

  $('prev-week').addEventListener('click', () => busy(null, async () => {
    state.weekStart = addDays(state.weekStart, -7); state.date = iso(state.weekStart); state.selected = null; await refresh();
  }));
  $('next-week').addEventListener('click', () => busy(null, async () => {
    state.weekStart = addDays(state.weekStart, 7); state.date = iso(state.weekStart); state.selected = null; await refresh();
  }));

  $('days').addEventListener('click', (e) => {
    const b = e.target.closest('.day');
    if (!b) return;
    busy(null, async () => { state.date = b.dataset.date; await refresh(); });
  });

  $('board').addEventListener('click', (e) => {
    const c = e.target.closest('.cell[data-area]');
    if (!c) return;
    state.selected = { areaId: Number(c.dataset.area), shiftId: Number(c.dataset.shift) };
    renderBoard();
    renderPanel();
  });

  $('panel').addEventListener('click', (e) => {
    const b = e.target.closest('button');
    if (!b) return;
    if (b.dataset.assign) assign(b, b.dataset.assign, b.dataset.name);
    else if (b.dataset.absent) markAbsent(b, b.dataset.absent, b.dataset.name);
    else if (b.dataset.remove) remove(b, b.dataset.remove);
  });

  $('generate-btn').addEventListener('click', (e) => generate(true, e.currentTarget));
  $('modal-apply').addEventListener('click', (e) => generate(false, e.currentTarget));
  $('modal-close').addEventListener('click', () => { $('modal').hidden = true; });
  $('modal').addEventListener('click', (e) => { if (e.target.id === 'modal') $('modal').hidden = true; });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') $('modal').hidden = true; });

  // ---------- boot ----------

  try {
    state.token = sessionStorage.getItem('shiftwise.token');
    state.role = sessionStorage.getItem('shiftwise.role');
  } catch (e) { state.token = null; }
  if (state.token) {
    $('who').textContent = (state.role || '').toLowerCase();
    start().catch(() => signOut());
  } else {
    $('login-view').hidden = false;
  }
})();
