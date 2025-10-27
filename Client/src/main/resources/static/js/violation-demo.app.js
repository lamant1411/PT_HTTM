// Mock video frame - sử dụng một trong các ảnh vi phạm làm background
const mockVideoFrame = '../violation_images/violation_057cb145-1496-468f-832d-a973e27aeba1.jpg';

let canvas, ctx;
let points = [];
let lineDrawn = false;

document.addEventListener('DOMContentLoaded', () => {
    canvas = document.getElementById('video-canvas');
    ctx = canvas.getContext('2d');
    
    // Load mock video frame
    loadMockFrame();
    
    // Setup event listeners
    canvas.addEventListener('click', handleCanvasClick);
    document.getElementById('line-form').addEventListener('submit', handleSubmit);
    document.getElementById('view-violation-btn').addEventListener('click', goToResults);
    
    console.log('%c🎬 DEMO MODE: Video Processing', 'color: #4a90e2; font-size: 16px; font-weight: bold;');
});

function loadMockFrame() {
    const img = new Image();
    img.onload = function() {
        // Vẽ ảnh lên canvas
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        
        // Vẽ overlay text
        ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
        ctx.fillRect(0, canvas.height - 40, canvas.width, 40);
        ctx.fillStyle = 'white';
        ctx.font = '16px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('📹 Click để vẽ vạch dừng', canvas.width / 2, canvas.height - 15);
    };
    img.src = mockVideoFrame;
}

function handleCanvasClick(e) {
    if (points.length >= 2) {
        // Reset nếu đã có 2 điểm
        points = [];
        lineDrawn = false;
        loadMockFrame();
        clearInputs();
        document.getElementById('submit-btn').disabled = true;
    }
    
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const x = Math.round((e.clientX - rect.left) * scaleX);
    const y = Math.round((e.clientY - rect.top) * scaleY);
    
    points.push({ x, y });
    
    // Vẽ điểm
    ctx.fillStyle = 'red';
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, 2 * Math.PI);
    ctx.fill();
    
    // Cập nhật input
    if (points.length === 1) {
        document.getElementById('x1').value = x;
        document.getElementById('y1').value = y;
    } else if (points.length === 2) {
        document.getElementById('x2').value = x;
        document.getElementById('y2').value = y;
        
        // Vẽ đường thẳng
        drawLine();
        lineDrawn = true;
        document.getElementById('submit-btn').disabled = false;
    }
}

function drawLine() {
    if (points.length !== 2) return;
    
    const [p1, p2] = points;
    
    ctx.strokeStyle = 'red';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
    
    // Vẽ label
    ctx.fillStyle = 'red';
    ctx.font = 'bold 14px Arial';
    ctx.fillText('STOP LINE', (p1.x + p2.x) / 2, (p1.y + p2.y) / 2 - 10);
}

function clearInputs() {
    document.getElementById('x1').value = '';
    document.getElementById('y1').value = '';
    document.getElementById('x2').value = '';
    document.getElementById('y2').value = '';
}

function handleSubmit(e) {
    e.preventDefault();
    
    if (!lineDrawn) {
        alert('⚠️ Vui lòng vẽ vạch dừng trước!');
        return;
    }
    
    // Disable button và hiện loading
    const submitBtn = document.getElementById('submit-btn');
    const loadingSpinner = document.getElementById('loading-spinner');
    const progressBar = document.getElementById('progress-bar');
    const progressFill = document.getElementById('progress-fill');
    
    submitBtn.disabled = true;
    loadingSpinner.style.display = 'block';
    progressBar.style.display = 'block';
    
    // Simulate progress
    let progress = 0;
    const progressInterval = setInterval(() => {
        progress += 10;
        progressFill.style.width = progress + '%';
        progressFill.textContent = progress + '%';
        
        if (progress >= 100) {
            clearInterval(progressInterval);
        }
    }, 300);
    
    // Simulate server processing (3 seconds)
    setTimeout(() => {
        loadingSpinner.style.display = 'none';
        progressBar.style.display = 'none';
        submitBtn.disabled = false;
        
        // Mock response từ server ML
        const mockViolations = [
            {
                frame: 125,
                video_time: '00:40',
                license_plate: '30A-12345',
                evidence_url: '../violation_images/violation_057cb145-1496-468f-832d-a973e27aeba1.jpg',
                timestamp: new Date().toISOString(),
                details: 'Vượt vạch dừng khi đèn đỏ'
            },
            {
                frame: 342,
                video_time: '01:25',
                license_plate: '29B-67890',
                evidence_url: '../violation_images/violation_2281b11a-ef82-4603-aa3f-d4e410b81a2f.jpg',
                timestamp: new Date().toISOString(),
                details: 'Vượt vạch dừng khi đèn đỏ'
            },
            {
                frame: 567,
                video_time: '02:13',
                license_plate: '51C-24680',
                evidence_url: '../violation_images/violation_5c720d93-4be9-4d84-afd5-2c64a7f0bb9b.jpg',
                timestamp: new Date().toISOString(),
                details: 'Vượt vạch dừng khi đèn đỏ'
            },
            {
                frame: 789,
                video_time: '03:08',
                license_plate: '92D-13579',
                evidence_url: '../violation_images/violation_73e34567-c961-437d-bbe6-49144455a04c.jpg',
                timestamp: new Date().toISOString(),
                details: 'Vượt vạch dừng khi đèn đỏ'
            },
            {
                frame: 1024,
                video_time: '04:52',
                license_plate: '15E-98765',
                evidence_url: '../violation_images/violation_c70223fc-a993-41d1-b011-5b734c13deb2.jpg',
                timestamp: new Date().toISOString(),
                details: 'Vượt vạch dừng khi đèn đỏ'
            }
        ];
        
        // Lưu vào sessionStorage giống như flow thật
        sessionStorage.setItem('violations', JSON.stringify(mockViolations));
        sessionStorage.setItem('lineData', JSON.stringify(points));
        
        // Enable nút xem kết quả
        document.getElementById('view-violation-btn').disabled = false;
        
        // Hiện thông báo
        alert(`✅ Xử lý xong!\n\nĐã phát hiện ${mockViolations.length} vi phạm.\n\nClick "Xem kết quả" để xem chi tiết.`);
        
        console.log('Mock violations generated:', mockViolations);
    }, 3000);
}

function goToResults() {
    window.location.href = 'results-demo.html';
}

// Thêm helper để reset demo
window.resetDemo = function() {
    points = [];
    lineDrawn = false;
    loadMockFrame();
    clearInputs();
    document.getElementById('submit-btn').disabled = true;
    document.getElementById('view-violation-btn').disabled = true;
    sessionStorage.clear();
    console.log('✅ Demo đã được reset!');
};

console.log('%cGọi resetDemo() để reset lại demo', 'color: #50c878; font-size: 14px;');
