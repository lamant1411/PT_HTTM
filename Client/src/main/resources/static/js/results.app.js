document.addEventListener('DOMContentLoaded', () => {
    const tableBody = document.querySelector('#results-table tbody');
    const noResults = document.getElementById('no-results');

    // Lấy chuỗi JSON log từ sessionStorage
    const logJsonString = sessionStorage.getItem('violationLogs');

    if (!logJsonString) {
        noResults.style.display = 'block';
        return;
    }

    try {
        const logs = JSON.parse(logJsonString);

        if (logs.length === 0) {
            noResults.style.display = 'block';
            return;
        }

        // Xây dựng các hàng của bảng
        logs.forEach(log => {
            const row = tableBody.insertRow();
            
            const cellTime = row.insertCell();
            cellTime.textContent = log.frame || 'N/A';
            
            const cellPlate = row.insertCell();
            cellPlate.textContent = log.license_plate || 'Không nhận diện được';
            
            const cellImage = row.insertCell();
            const img = document.createElement('img');
            img.src = log.evidence_url; // Đường dẫn /violation_images/...
            img.alt = `Bằng chứng frame ${log.frame}`;
            
            // Tùy chọn: Click vào ảnh để xem to hơn
            img.addEventListener('click', () => {
                window.open(log.evidence_url, '_blank');
            });
            
            cellImage.appendChild(img);
        });

    } catch (e) {
        console.error("Lỗi parse JSON:", e);
        noResults.textContent = "Lỗi hiển thị kết quả.";
        noResults.style.display = 'block';
    }

    // Xóa log khỏi storage sau khi đã hiển thị
    // sessionStorage.removeItem('violationLogs'); 
    // (Bạn có thể giữ lại để khi F5 không bị mất)
});