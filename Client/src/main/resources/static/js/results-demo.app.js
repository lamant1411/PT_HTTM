// Dữ liệu mock cho demo
const mockViolations = [
    {
        frame: 125,
        video_time: '00:40',
        license_plate: '30A-12345',
        evidence_url: '../violation_images/violation_057cb145-1496-468f-832d-a973e27aeba1.jpg',
        timestamp: '2024-01-15 14:23:45',
        details: 'Vượt vạch dừng khi đèn đỏ'
    },
    {
        frame: 342,
        video_time: '01:25',
        license_plate: '29B-67890',
        evidence_url: '../violation_images/violation_2281b11a-ef82-4603-aa3f-d4e410b81a2f.jpg',
        timestamp: '2024-01-15 14:25:12',
        details: 'Vượt vạch dừng khi đèn đỏ'
    },
    {
        frame: 567,
        video_time: '02:13',
        license_plate: '51C-24680',
        evidence_url: '../violation_images/violation_5c720d93-4be9-4d84-afd5-2c64a7f0bb9b.jpg',
        timestamp: '2024-01-15 14:27:33',
        details: 'Vượt vạch dừng khi đèn đỏ'
    },
    {
        frame: 789,
        video_time: '03:08',
        license_plate: '92D-13579',
        evidence_url: '../violation_images/violation_73e34567-c961-437d-bbe6-49144455a04c.jpg',
        timestamp: '2024-01-15 14:29:01',
        details: 'Vượt vạch dừng khi đèn đỏ'
    },

];

let violations = [...mockViolations]; // Copy để có thể xóa

document.addEventListener('DOMContentLoaded', () => {
    renderTable();
    setupEventListeners();
});

function renderTable() {
    const tableBody = document.getElementById('table-body');
    tableBody.innerHTML = '';

    if (violations.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px; color: #999;">Không có dữ liệu vi phạm</td></tr>';
        return;
    }

    violations.forEach((log, index) => {
        const row = document.createElement('tr');
        row.dataset.index = index;
        
        // Checkbox column
        const cellCheckbox = document.createElement('td');
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'violation-checkbox';
        checkbox.dataset.index = index;
        checkbox.style.cursor = 'pointer';
        cellCheckbox.appendChild(checkbox);
        
        // STT column
        const cellNo = document.createElement('td');
        cellNo.textContent = index + 1;
        
        // Frame column
        const cellTime = document.createElement('td');
        cellTime.textContent = log.video_time || `Frame ${log.frame}`;
        cellTime.style.fontFamily = 'monospace';
        cellTime.style.fontSize = '16px';
        
        // License Plate column
        const cellPlate = document.createElement('td');
        cellPlate.textContent = log.license_plate;
        cellPlate.style.fontWeight = 'bold';
        cellPlate.style.color = '#007bff';
        
        // Image column
        const cellImage = document.createElement('td');
        const img = document.createElement('img');
        img.src = log.evidence_url;
        img.alt = 'Evidence';
        img.style.maxWidth = '150px';
        img.style.cursor = 'pointer';
        img.style.borderRadius = '4px';
        img.addEventListener('click', () => window.open(log.evidence_url, '_blank'));
        cellImage.appendChild(img);
        
        // Action column with individual delete button
        const cellAction = document.createElement('td');
        const deleteBtn = document.createElement('button');
        deleteBtn.textContent = 'Xóa';
        deleteBtn.style.cssText = 'background:#ffc107;color:white;border:none;padding:5px 15px;border-radius:3px;cursor:pointer;font-size:14px;';
        deleteBtn.addEventListener('click', () => deleteIndividual(index));
        cellAction.appendChild(deleteBtn);
        
        row.appendChild(cellCheckbox);
        row.appendChild(cellNo);
        row.appendChild(cellTime);
        row.appendChild(cellPlate);
        row.appendChild(cellImage);
        row.appendChild(cellAction);
        
        tableBody.appendChild(row);
    });
}

function setupEventListeners() {
    // Select All checkbox
    const selectAllCheckbox = document.getElementById('select-all');
    selectAllCheckbox.addEventListener('change', function() {
        const checkboxes = document.querySelectorAll('.violation-checkbox');
        checkboxes.forEach(cb => cb.checked = this.checked);
    });

    // Save Selected button
    document.getElementById('save-selected-btn').addEventListener('click', () => {
        const selectedCheckboxes = document.querySelectorAll('.violation-checkbox:checked');
        if (selectedCheckboxes.length === 0) {
            alert('⚠️ Vui lòng chọn ít nhất một vi phạm để lưu!');
            return;
        }

        const selectedViolations = Array.from(selectedCheckboxes).map(cb => {
            const index = parseInt(cb.dataset.index);
            return violations[index];
        });

        if (confirm(`Bạn có chắc muốn lưu ${selectedViolations.length} vi phạm đã chọn vào cơ sở dữ liệu?`)) {
            // Giả lập việc gọi API
            console.log('Saving selected violations:', selectedViolations);
            alert(`✅ Đã lưu ${selectedViolations.length} vi phạm vào database!\n\n(Đây là chế độ demo - dữ liệu không thực sự được lưu)`);
            
            // Bỏ chọn tất cả sau khi lưu
            selectedCheckboxes.forEach(cb => cb.checked = false);
            document.getElementById('select-all').checked = false;
        }
    });

    // Save All button
    document.getElementById('save-all-btn').addEventListener('click', () => {
        if (violations.length === 0) {
            alert('⚠️ Không có dữ liệu để lưu!');
            return;
        }

        if (confirm(`Bạn có chắc muốn lưu tất cả ${violations.length} vi phạm vào cơ sở dữ liệu?`)) {
            // Giả lập việc gọi API
            console.log('Saving all violations:', violations);
            alert(`✅ Đã lưu tất cả ${violations.length} vi phạm vào database!\n\n(Đây là chế độ demo - dữ liệu không thực sự được lưu)`);
        }
    });

    // Delete Selected button
    document.getElementById('delete-selected-btn').addEventListener('click', () => {
        const selectedCheckboxes = document.querySelectorAll('.violation-checkbox:checked');
        if (selectedCheckboxes.length === 0) {
            alert('⚠️ Vui lòng chọn ít nhất một vi phạm để xóa!');
            return;
        }

        if (confirm(`Bạn có chắc muốn xóa ${selectedCheckboxes.length} vi phạm đã chọn?`)) {
            // Lấy các index và sắp xếp giảm dần để xóa từ cuối lên
            const indices = Array.from(selectedCheckboxes)
                .map(cb => parseInt(cb.dataset.index))
                .sort((a, b) => b - a);

            // Xóa từng phần tử
            indices.forEach(index => {
                violations.splice(index, 1);
            });

            alert(`✅ Đã xóa ${indices.length} vi phạm khỏi danh sách!`);
            document.getElementById('select-all').checked = false;
            renderTable();
        }
    });

    // Clear All button
    document.getElementById('clear-all-btn').addEventListener('click', () => {
        if (violations.length === 0) {
            alert('⚠️ Danh sách đã trống!');
            return;
        }

        if (confirm(`Bạn có chắc muốn xóa tất cả ${violations.length} vi phạm?`)) {
            violations = [];
            alert('✅ Đã xóa tất cả dữ liệu!');
            renderTable();
        }
    });
}

function deleteIndividual(index) {
    const violation = violations[index];
    if (confirm(`Xóa vi phạm: ${violation.license_plate} (${violation.video_time})?`)) {
        violations.splice(index, 1);
        alert('✅ Đã xóa vi phạm!');
        renderTable();
    }
}

// Thêm một nút để reset lại dữ liệu demo
window.resetDemo = function() {
    if (confirm('Reset lại dữ liệu demo về ban đầu?')) {
        violations = [...mockViolations];
        document.getElementById('select-all').checked = false;
        renderTable();
        alert('✅ Đã reset dữ liệu demo!');
    }
};

// Thêm thông báo vào console
console.log('%c🎭 DEMO MODE ACTIVATED', 'color: #dc3545; font-size: 20px; font-weight: bold;');
console.log('%cGọi resetDemo() để reset lại dữ liệu ban đầu', 'color: #007bff; font-size: 14px;');
console.log('Mock violations:', mockViolations);
