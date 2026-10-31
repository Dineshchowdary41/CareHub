/**
 * Generic CRUD table + modal-form engine.
 * Each entity page defines a small config object and calls CRUD.init(config).
 *
 * config = {
 *   title, subtitle, endpoint ('/patients' etc.), idField ('patientId'),
 *   columns: [{ key, label, render(row) -> string }],
 *   fields:  [{ name, label, type: text|number|date|datetime-local|textarea|select|fk,
 *               required, options: [strings] (for select),
 *               fkEndpoint, fkIdField, fkLabel(row)->string (for fk) }],
 *   searchKeys: [column keys used for the search box]
 * }
 */
const CRUD = (() => {
  let cfg = null;
  let rows = [];
  let fkOptionsCache = {}; // fieldName -> list of records
  let editingId = null;
  let filteredRowsCache = [];

  function $(id) { return document.getElementById(id); }

  function showToast(message, isError) {
    const t = $("toast");
    t.textContent = message;
    t.className = "toast show" + (isError ? " error" : "");
    setTimeout(() => { t.className = "toast"; }, 2600);
  }

  async function loadFkOptions() {
    const fkFields = cfg.fields.filter(f => f.type === "fk");
    for (const f of fkFields) {
      try {
        fkOptionsCache[f.name] = await Api.get(f.fkEndpoint);
      } catch (e) {
        fkOptionsCache[f.name] = [];
      }
    }
  }

  function renderTableHead() {
    const head = $("tableHead");
    const ths = cfg.columns.map(c => `<th>${c.label}</th>`).join("");
    head.innerHTML = `<tr>${ths}<th>Actions</th></tr>`;
  }

  function renderTableBody(list) {
    filteredRowsCache = list;
    const body = $("tableBody");
    if (!list.length) {
      body.innerHTML = `<tr class="empty-row"><td colspan="${cfg.columns.length + 1}">No records yet — click "Add" to create one.</td></tr>`;
      return;
    }
    body.innerHTML = list.map((row, idx) => {
      const tds = cfg.columns.map(c => `<td>${c.render ? c.render(row) : (row[c.key] ?? "")}</td>`).join("");
      const id = row[cfg.idField];
      const extra = (cfg.rowActions || []).map((a, ai) => {
        if (a.visible && !a.visible(row)) return "";
        const cls = typeof a.className === "function" ? a.className(row) : (a.className || "btn-secondary");
        return `<button class="btn ${cls} btn-sm" onclick="CRUD.runRowAction(${idx}, ${ai})">${typeof a.label === "function" ? a.label(row) : a.label}</button>`;
      }).join("");
      const defaultActions = cfg.noDefaultActions ? "" : `
          <button class="btn btn-secondary btn-sm" onclick="CRUD.openEdit(${id})">Edit</button>
          <button class="btn btn-danger btn-sm" onclick="CRUD.remove(${id})">Delete</button>`;
      return `<tr>
        ${tds}
        <td class="actions-cell">${defaultActions}${extra}</td>
      </tr>`;
    }).join("");
  }

  async function runRowAction(rowIdx, actionIdx) {
    const row = filteredRowsCache[rowIdx];
    const action = (cfg.rowActions || [])[actionIdx];
    if (!row || !action) return;
    try {
      await action.handler(row);
      await refresh();
    } catch (e) {
      showToast(e.message, true);
    }
  }

  async function refresh() {
    try {
      rows = await Api.get(cfg.endpoint);
      applyFilter();
    } catch (e) {
      showToast(e.message, true);
    }
  }

  function applyFilter() {
    const q = ($("searchInput").value || "").toLowerCase().trim();
    if (!q) { renderTableBody(rows); return; }
    const filtered = rows.filter(row => {
      return cfg.searchKeys.some(key => {
        const col = cfg.columns.find(c => c.key === key);
        const val = col && col.render ? col.render(row) : row[key];
        return String(val ?? "").toLowerCase().includes(q);
      });
    });
    renderTableBody(filtered);
  }

  function fieldInputHtml(f, value) {
    const req = f.required ? "required" : "";
    if (f.type === "select") {
      const opts = f.options.map(o => `<option value="${o}" ${value === o ? "selected" : ""}>${o}</option>`).join("");
      return `<select id="fld_${f.name}" ${req}><option value="">-- select --</option>${opts}</select>`;
    }
    if (f.type === "fk") {
      const list = fkOptionsCache[f.name] || [];
      const opts = list.map(item => {
        const id = item[f.fkIdField];
        const selected = value == id ? "selected" : "";
        return `<option value="${id}" ${selected}>${f.fkLabel(item)}</option>`;
      }).join("");
      const emptyLabel = f.required ? "-- select --" : "-- none --";
      return `<select id="fld_${f.name}" ${req}><option value="">${emptyLabel}</option>${opts}</select>`;
    }
    if (f.type === "textarea") {
      return `<textarea id="fld_${f.name}" ${req}>${value ?? ""}</textarea>`;
    }
    return `<input type="${f.type}" id="fld_${f.name}" value="${value ?? ""}" ${req} />`;
  }

  function openModal(title, record) {
    $("modalTitle").textContent = title;
    const visibleFields = cfg.fields.filter(f => !(record && f.createOnly));
    const formHtml = visibleFields.map(f => {
      let value = "";
      if (record) {
        if (f.type === "fk") {
          value = record[f.name] ? record[f.name][f.fkIdField] : "";
        } else {
          value = record[f.name] ?? "";
        }
      } else if (f.default !== undefined) {
        value = f.default;
      }
      return `<div class="form-group">
        <label>${f.label}${f.required ? " *" : ""}</label>
        ${fieldInputHtml(f, value)}
      </div>`;
    }).join("");
    $("modalForm").innerHTML = formHtml;
    $("modalOverlay").classList.add("open");
  }

  function closeModal() {
    $("modalOverlay").classList.remove("open");
    editingId = null;
  }

  function collectPayload() {
    const payload = {};
    for (const f of cfg.fields) {
      const el = $("fld_" + f.name);
      if (!el) continue; // field hidden (createOnly during edit)
      let raw = el.value;
      if (f.type === "fk") {
        payload[f.name] = raw ? { [f.fkIdField]: Number(raw) } : null;
      } else if (f.type === "number") {
        payload[f.name] = raw === "" ? null : Number(raw);
      } else {
        payload[f.name] = raw === "" ? null : raw;
      }
    }
    return payload;
  }

  async function save() {
    try {
      const payload = collectPayload();
      if (editingId) {
        await Api.put(`${cfg.endpoint}/${editingId}`, payload);
        showToast("Updated successfully");
      } else {
        await Api.post(cfg.endpoint, payload);
        showToast("Created successfully");
      }
      closeModal();
      await refresh();
    } catch (e) {
      showToast(e.message, true);
    }
  }

  function openCreate() {
    editingId = null;
    openModal(`Add ${cfg.title.replace(/s$/, "")}`, null);
  }

  function openEdit(id) {
    editingId = id;
    const record = rows.find(r => r[cfg.idField] == id);
    openModal(`Edit ${cfg.title.replace(/s$/, "")}`, record);
  }

  async function remove(id) {
    if (!confirm("Delete this record? This cannot be undone.")) return;
    try {
      await Api.del(`${cfg.endpoint}/${id}`);
      showToast("Deleted");
      await refresh();
    } catch (e) {
      showToast(e.message, true);
    }
  }

  async function init(config) {
    cfg = config;
    $("pageTitle").textContent = cfg.title;
    $("pageSubtitle").textContent = cfg.subtitle || "";
    renderTableHead();
    $("addBtn").addEventListener("click", openCreate);
    $("searchInput").addEventListener("input", applyFilter);
    $("saveBtn").addEventListener("click", save);
    $("cancelBtn").addEventListener("click", closeModal);
    await loadFkOptions();
    await refresh();
  }

  return { init, openEdit, remove, closeModal, runRowAction };
})();
