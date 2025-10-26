document.getElementById('uploadForm').addEventListener('submit', function(e) {
  e.preventDefault();
  const form = e.target;
  const fd = new FormData(form);

  fetch('/api/process/upload', { method: 'POST', body: fd })
    .then(r => r.json())
    .then(data => { document.getElementById('result').textContent = JSON.stringify(data, null, 2); })
    .catch(err => { document.getElementById('result').textContent = err; });
});
