document.addEventListener('DOMContentLoaded', () => {
    const tableBody = document.querySelector('#results-table tbody');
    const noResults = document.getElementById('no-results');
    const videoElement = document.getElementById('result-video');
    const saveAllBtn = document.getElementById('save-all-btn');
    const clearAllBtn = document.getElementById('clear-all-btn');

    let logs = [];

    // Kiểm tra xem có video data không (có thể không có nếu video quá lớn)
    const videoData = sessionStorage.getItem('uploadedVideoData');
    const videoName = sessionStorage.getItem('uploadedVideoName');
    
    if (videoData) {
        videoElement.src = videoData;
        videoElement.style.display = 'block';
    } else if (videoName) {
        // Hiển thị thông báo video không khả dụng
        const videoContainer = videoElement.parentElement;
        const notice = document.createElement('p');
        notice.style.cssText = 'color:#666;font-style:italic;text-align:center;';
        notice.textContent = `Video "${videoName}" quá lớn để hiển thị (vượt quá giới hạn bộ nhớ trình duyệt)`;
        videoContainer.insertBefore(notice, videoElement);
        videoElement.style.display = 'none';
    }

    // Load violation logs
    const logJsonString = sessionStorage.getItem('violationLogs');
    if (!logJsonString) {
        noResults.style.display = 'block';
        if (saveAllBtn) saveAllBtn.disabled = true;
        return;
    }

    try {
        logs = JSON.parse(logJsonString);
        if (logs.length === 0) {
            noResults.style.display = 'block';
            if (saveAllBtn) saveAllBtn.disabled = true;
            return;
        }

        // Render table rows
        logs.forEach((log, index) => {
            const row = tableBody.insertRow();
            
            // STT column
            const cellNo = row.insertCell();
            cellNo.textContent = index + 1;
            
            // Frame column
            const cellTime = row.insertCell();
            cellTime.textContent = log.frame || 'N/A';
            
            // License Plate column
            const cellPlate = row.insertCell();
            cellPlate.textContent = log.license_plate || 'Không nhận diện được';
            
            // Image column
            const cellImage = row.insertCell();
            const img = document.createElement('img');
            img.src = log.evidence_url;
            img.alt = 'Evidence';
            img.style.maxWidth = '150px';
            img.style.cursor = 'pointer';
            img.addEventListener('click', () => window.open(log.evidence_url, '_blank'));
            cellImage.appendChild(img);
            
            // Action column with delete button
            const cellAction = row.insertCell();
            const deleteBtn = document.createElement('button');
            deleteBtn.textContent = 'Xóa';
            deleteBtn.style.cssText = 'background:#ffc107;color:white;border:none;padding:5px 10px;border-radius:3px;cursor:pointer';
            deleteBtn.addEventListener('click', () => {
                if (confirm('Xóa vi phạm này?')) {
                    logs.splice(index, 1);
                    sessionStorage.setItem('violationLogs', JSON.stringify(logs));
                    location.reload();
                }
            });
            cellAction.appendChild(deleteBtn);
        });
    } catch (e) {
        console.error("Lỗi parse JSON:", e);
        noResults.textContent = "Lỗi hiển thị kết quả.";
        noResults.style.display = 'block';
    }

    // Save All button handler
    if (saveAllBtn) {
        saveAllBtn.addEventListener('click', async () => {
            if (logs.length === 0) {
                alert('Không có dữ liệu để lưu!');
                return;
            }
            if (!confirm('Lưu ' + logs.length + ' vi phạm vào cơ sở dữ liệu?')) return;

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

                alert('Đã lưu ' + savedCount + '/' + logs.length + ' vi phạm vào CSDL!');
                sessionStorage.clear();
                window.location.href = '/html/history.html';
            } catch (error) {
                alert('Lỗi khi lưu: ' + error.message);
                saveAllBtn.disabled = false;
                saveAllBtn.textContent = 'Lưu tất cả vào CSDL';
            }
        });
    }

    // Clear All button handler
    if (clearAllBtn) {
        clearAllBtn.addEventListener('click', () => {
            if (confirm('Xóa tất cả kết quả và quay về trang upload?')) {
                sessionStorage.clear();
                window.location.href = '/html/violation.html';
            }
        });
    }

    // Cleanup video data when leaving page (nếu có)
    window.addEventListener('beforeunload', () => {
        sessionStorage.removeItem('uploadedVideoData');
        sessionStorage.removeItem('uploadedVideoName');
        sessionStorage.removeItem('uploadedVideoType');
    });
});
