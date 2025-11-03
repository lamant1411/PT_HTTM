document.addEventListener('DOMContentLoaded', () => {
    const tableBody = document.querySelector('#results-table tbody');
    const noResults = document.getElementById('no-results');
    const videoElement = document.getElementById('result-video');
    const videoPlaceholder = document.getElementById('video-placeholder');
    const saveAllBtn = document.getElementById('save-all-btn');
    const saveSelectedBtn = document.getElementById('save-selected-btn');
    const deleteSelectedBtn = document.getElementById('delete-selected-btn');
    const clearAllBtn = document.getElementById('clear-all-btn');
    const selectAllCheckbox = document.getElementById('select-all');

    let logs = [];

    videoPlaceholder.innerHTML = `<p style="color:#666;font-size:16px;margin:0;">videoName</p><p style="color:#999;font-size:14px;margin:10px 0 0 0;">Chức năng xem video hiện không khả dụng</p>`;
    // Load violation logs
    const logJsonString = sessionStorage.getItem('violationLogs');
    if (!logJsonString) {
        noResults.style.display = 'block';
        if (saveAllBtn) saveAllBtn.disabled = true;
        if (saveSelectedBtn) saveSelectedBtn.disabled = true;
        return;
    }

    try {
        logs = JSON.parse(logJsonString);
        if (logs.length === 0) {
            noResults.style.display = 'block';
            if (saveAllBtn) saveAllBtn.disabled = true;
            if (saveSelectedBtn) saveSelectedBtn.disabled = true;
            return;
        }

        // Render các hàng
        logs.forEach((log, index) => {
            const row = tableBody.insertRow();
            
            // Checkbox
            const cellCheckbox = row.insertCell();
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'violation-checkbox';
            checkbox.dataset.index = index;
            checkbox.style.cursor = 'pointer';
            cellCheckbox.appendChild(checkbox);
            
            // STT
            const cellNo = row.insertCell();
            cellNo.textContent = index + 1;
            
            // Time column (hiển thị thời gian trong video)
            const cellTime = row.insertCell();
            cellTime.textContent = log.video_time || 'N/A';
            cellTime.style.fontFamily = 'monospace';
            cellTime.style.fontSize = '16px';
            
            // Biển số xe
            const cellPlate = row.insertCell();
            cellPlate.textContent = log.license_plate || 'Không nhận diện được';
            cellPlate.style.fontWeight = 'bold';
            cellPlate.style.color = '#007bff';
            
            // Hình ảnh vi phạm
            const cellImage = row.insertCell();
            const img = document.createElement('img');
            // Dùng base64, fallback về URL nếu đã lưu DB
            img.src = log.evidence_image_base64 || log.evidence_url || '';
            img.alt = 'Evidence';
            img.style.maxWidth = '150px';
            img.style.cursor = 'pointer';
            img.style.borderRadius = '4px';
            img.addEventListener('click', () => {
                const imageSrc = log.evidence_image_base64 || log.evidence_url;
                if (imageSrc) window.open(imageSrc, '_blank');
            });
            cellImage.appendChild(img);
            
            // Cột Action với nút xóa cho từng hàng
            const cellAction = row.insertCell();
            const deleteBtn = document.createElement('button');
            deleteBtn.textContent = 'Xóa';
            deleteBtn.style.cssText = 'background:#f5a623;color:white;border:none;padding:5px 15px;border-radius:3px;cursor:pointer;font-size:14px;';
            deleteBtn.addEventListener('click', () => deleteIndividual(index));
            cellAction.appendChild(deleteBtn);
        });
    } catch (e) {
        console.error("Lỗi parse JSON:", e);
        noResults.textContent = "Lỗi hiển thị kết quả.";
        noResults.style.display = 'block';
    }

    // Chọn tất cả
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('.violation-checkbox');
            checkboxes.forEach(cb => cb.checked = this.checked);
        });
    }

    // Lưu những mục đã chọn
    if (saveSelectedBtn) {
        saveSelectedBtn.addEventListener('click', async () => {
            const selectedCheckboxes = document.querySelectorAll('.violation-checkbox:checked');
            if (selectedCheckboxes.length === 0) {
                alert('Vui lòng chọn ít nhất một vi phạm để lưu!');
                return;
            }

            const selectedLogs = Array.from(selectedCheckboxes).map(cb => {
                const index = parseInt(cb.dataset.index);
                return logs[index];
            });

            if (!confirm(`Lưu ${selectedLogs.length} vi phạm đã chọn vào cơ sở dữ liệu?`)) return;

            saveSelectedBtn.disabled = true;
            saveSelectedBtn.textContent = 'Đang lưu...';

            try {
                let savedCount = 0;
                for (const log of selectedLogs) {
                    const response = await fetch('/api/v1/violations/save', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(log)
                    });
                    if (response.ok) savedCount++;
                }

                alert(`Đã lưu ${savedCount}/${selectedLogs.length} vi phạm vào CSDL!`);
                
                // Bỏ chọn sau khi lưu
                selectedCheckboxes.forEach(cb => cb.checked = false);
                if (selectAllCheckbox) selectAllCheckbox.checked = false;
                
                saveSelectedBtn.disabled = false;
                saveSelectedBtn.textContent = 'Lưu các mục đã chọn';
            } catch (error) {
                alert('Lỗi khi lưu: ' + error.message);
                saveSelectedBtn.disabled = false;
                saveSelectedBtn.textContent = 'Lưu các mục đã chọn';
            }
        });
    }

    // Lưu toàn bộ
    if (saveAllBtn) {
        saveAllBtn.addEventListener('click', async () => {
            if (logs.length === 0) {
                alert('Không có dữ liệu để lưu!');
                return;
            }
            if (!confirm('Lưu tất cả ' + logs.length + ' vi phạm vào cơ sở dữ liệu?')) return;

            saveAllBtn.disabled = true;
            saveAllBtn.textContent = 'Đang lưu...';

            try {
                let savedCount = 0;
                for (const log of logs) {
                    const response = await fetch('/api/v1/violations/save', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(log)
                    });
                    if (response.ok) savedCount++;
                }

                alert(`Đã lưu ${savedCount}/${logs.length} vi phạm vào CSDL!`);
                saveAllBtn.disabled = false;
                saveAllBtn.textContent = 'Lưu tất cả';
            } catch (error) {
                alert('Lỗi khi lưu: ' + error.message);
                saveAllBtn.disabled = false;
                saveAllBtn.textContent = 'Lưu tất cả';
            }
        });
    }

    // Xóa những mục đã chọn
    if (deleteSelectedBtn) {
        deleteSelectedBtn.addEventListener('click', () => {
            const selectedCheckboxes = document.querySelectorAll('.violation-checkbox:checked');
            if (selectedCheckboxes.length === 0) {
                alert('Vui lòng chọn ít nhất một vi phạm để xóa!');
                return;
            }

            if (!confirm(`Bạn có chắc muốn xóa ${selectedCheckboxes.length} vi phạm đã chọn?`)) {
                return;
            }

            // Lấy các index và sắp xếp giảm dần để xóa từ cuối lên
            const indices = Array.from(selectedCheckboxes)
                .map(cb => parseInt(cb.dataset.index))
                .sort((a, b) => b - a);

            // Xóa từng phần tử
            indices.forEach(index => {
                logs.splice(index, 1);
            });

            sessionStorage.setItem('violationLogs', JSON.stringify(logs));
            alert(`Đã xóa ${indices.length} vi phạm khỏi danh sách!`);
            location.reload();
        });
    }

    // Xóa tất cả
    if (clearAllBtn) {
        clearAllBtn.addEventListener('click', () => {
            if (logs.length === 0) {
                alert('Danh sách đã trống!');
                return;
            }

            if (confirm('Bạn có chắc muốn xóa tất cả ' + logs.length + ' vi phạm?')) {
                sessionStorage.removeItem('violationLogs');
                alert('Đã xóa tất cả dữ liệu!');
                window.location.href = 'violation.html';
            }
        });
    }

    // Xóa từng mục
    function deleteIndividual(index) {
        const violation = logs[index];
        const identifier = violation.video_time || `Frame ${violation.frame}` || `#${index + 1}`;
        if (confirm(`Xóa vi phạm: ${violation.license_plate || 'N/A'} (${identifier})?`)) {
            logs.splice(index, 1);
            sessionStorage.setItem('violationLogs', JSON.stringify(logs));
            alert('Đã xóa vi phạm!');
            location.reload();
        }
    }
});
