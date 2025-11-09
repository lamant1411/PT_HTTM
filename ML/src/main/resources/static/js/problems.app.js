(function(){
  const API = '/api/problems';

  async function fetchJson(url, opts){
    const res = await fetch(url, Object.assign({ headers: {'Content-Type':'application/json'} }, opts));
    if (!res.ok) throw new Error('HTTP '+res.status+' '+await res.text());
    return res.json();
  }

  async function loadList(){
    try{
      const list = await fetchJson(API);
      const tbody = document.getElementById('problemsBody');
      tbody.innerHTML = '';
      list.forEach(p => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${p.id || ''}</td>
          <td>${escapeHtml(p.name || '')}</td>
          <td>${escapeHtml(p.description || '')}</td>
          <td class="action-cell">
            <button class="btn small" data-id="${p.id}" data-action="delete">Delete</button>
            <button class="btn small" data-id="${p.id}" data-action="detail">Detail</button>
            <button class="btn small" data-id="${p.id}" data-action="data">Data</button>
            <button class="btn small" data-id="${p.id}" data-action="model">Model</button>
          </td>`;
        tbody.appendChild(tr);
      });

      tbody.querySelectorAll('button').forEach(b => b.addEventListener('click', (ev)=>{
        const id = ev.currentTarget.getAttribute('data-id');
        const action = ev.currentTarget.getAttribute('data-action');
        if (action === 'delete'){
          if (confirm('Delete problem '+id+'?')) deleteProblem(id).then(loadList).catch(alertErr);
        } else if (action === 'detail'){
          window.location.href = '/problem.html?id='+id;
        } else if (action === 'data'){
          alert('Open data for problem '+id);
        } else if (action === 'model'){
          alert('Open model page for problem '+id);
        }
      }));

    }catch(err){
      alert('Load failed: '+err.message);
    }
  }

  async function loadDetail(id){
    try{
      console.log('Loading detail for id:', id);
      const p = await fetchJson(API + '/' + id);
      console.log('Received problem:', p);
      
      const titleEl = document.getElementById('title');
      const pidEl = document.getElementById('pid');
      const pdescEl = document.getElementById('pdesc');
      
      console.log('Elements:', {titleEl, pidEl, pdescEl});
      
      if (titleEl) titleEl.innerText = p.name || '(Tên bài toán)';
      if (pidEl) pidEl.innerText = p.id || '';
      if (pdescEl) pdescEl.innerText = p.description || '';
      
      return p;
    }catch(err){
      console.error('Load detail error:', err);
      alert('Failed to load detail: '+err.message);
      return null;
    }
  }

  async function create(problem){
    try{
      const res = await fetchJson(API, { method: 'POST', body: JSON.stringify(problem) });
      return res;
    }catch(err){ alertErr(err); throw err; }
  }

  async function update(id, problem){
    try{
      const res = await fetchJson(API + '/' + id, { method: 'PUT', body: JSON.stringify(problem) });
      return res;
    }catch(err){ alertErr(err); throw err; }
  }

  async function deleteProblem(id){
    try{
      const res = await fetchJson(API + '/' + id, { method: 'DELETE' });
      return res;
    }catch(err){ alertErr(err); throw err; }
  }

  function alertErr(err){
    alert('Error: '+ (err.message || err));
  }

  function escapeHtml(str){
    return String(str).replace(/[&<>"]+/g, function(s){
      return { '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[s];
    });
  }

  // expose
  window.app = { loadList, loadDetail, create, update, delete: deleteProblem };
})();
