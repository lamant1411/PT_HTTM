document.addEventListener('DOMContentLoaded', () => {

    const videoUpload = document.getElementById('video-upload');
    const uploadPrompt = document.getElementById('upload-prompt');
    const videoContainer = document.querySelector('.video-container');
    
    const x1_input = document.getElementById('x1');
    const y1_input = document.getElementById('y1');
    const x2_input = document.getElementById('x2');
    const y2_input = document.getElementById('y2');
    
    const submitBtn = document.getElementById('submit-btn');
    const loadingSpinner = document.getElementById('loading-spinner');

    let videoFile = null;
    let canvas = null;
    let ctx = null;
    let points = [];
    let originalFrame = new Image();
    let isProcessing = false;

    //--- Function xử lý click trên canvas ---
    function handleCanvasClick(e) {
        // Nếu đã có 2 điểm: reset và vẽ lại
        if (points.length >= 2) {
            points = [];
            ctx.drawImage(originalFrame, 0, 0, canvas.width, canvas.height);
            x1_input.value = ''; y1_input.value = '';
            x2_input.value = ''; y2_input.value = '';
            submitBtn.disabled = true;
        }

        const rect = canvas.getBoundingClientRect(); // Lấy kích thước hiển thị của canvas
        // Tính tỷ lệ scale giữa canvas thực tế và hiển thị
        const scaleX = canvas.width / rect.width;
        const scaleY = canvas.height / rect.height;
        
        // Tọa độ click trên canvas hiển thị
        const clickX = e.clientX - rect.left;
        const clickY = e.clientY - rect.top;
        
        // Chuyển đổi sang tọa độ thực tế của canvas
        const x = Math.round(clickX * scaleX);
        const y = Math.round(clickY * scaleY);
        
        points.push({ x, y });
        
        // Vẽ điểm đỏ
        ctx.fillStyle = 'red';
        ctx.beginPath();
        ctx.arc(x, y, 2, 0, 2 * Math.PI);
        ctx.fill();

        if (points.length === 1) {
            x1_input.value = x;
            y1_input.value = y;
        }

        if (points.length === 2) {
            x2_input.value = x;
            y2_input.value = y;
            
            // Vẽ đường nối
            ctx.beginPath();
            ctx.moveTo(points[0].x, points[0].y);
            ctx.lineTo(points[1].x, points[1].y);
            ctx.strokeStyle = 'red';
            ctx.lineWidth = 3;
            ctx.stroke();
            
            submitBtn.disabled = false;
        }
    }
    
    // --- Function xử lý video ---
    function processVideo(file) {
        isProcessing = true;
        console.log('Bắt đầu xử lý video:', file.name);
        console.log('Đang xử lý video...');

        // Tạo canvas
        canvas = document.createElement('canvas');
            canvas.style.maxWidth = '100%';
            canvas.style.height = 'auto';
            canvas.style.cursor = 'crosshair'; // đổi con trỏ chuột thành dấu +
            canvas.style.display = 'block';
            ctx = canvas.getContext('2d');
            
            // Xóa upload prompt và thêm canvas
            videoContainer.innerHTML = '';
            videoContainer.appendChild(canvas);
            
            // Gán sự kiện click (chỉ 1 lần)
            canvas.addEventListener('click', handleCanvasClick);
        
        // Reset thông tin 2 điểm có trước
        points = [];
        x1_input.value = ''; y1_input.value = '';
        x2_input.value = ''; y2_input.value = '';
        submitBtn.disabled = true;
        
        // Tạo video element mới để load video
        const video = document.createElement('video');
        video.style.display = 'none';
        video.preload = 'metadata';
        
        const fileURL = URL.createObjectURL(file);
        video.src = fileURL;
        
        // Load video và vẽ frame đầu tiên
        video.onloadedmetadata = () => {
            console.log('Video metadata:', video.videoWidth, 'x', video.videoHeight);
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            video.currentTime = 0.5;
        };
        
        video.onseeked = () => {
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            originalFrame.src = canvas.toDataURL();
            
            // Cleanup
            URL.revokeObjectURL(fileURL);
            video.remove();
            isProcessing = false;
        };
        
        video.onerror = (e) => {
            console.error('Lỗi load video:', e);
            alert('Không thể load video. Vui lòng thử file khác.');
            isProcessing = false;
            URL.revokeObjectURL(fileURL);
            video.remove();
        };
        
        // Load video
        video.load();
    }

    // --- Upload Video ---
    uploadPrompt.addEventListener('click', () => {
        // videoUpload.value = ''; // Reset input trước khi chọn lại
        videoUpload.click();
    });
    
    videoUpload.addEventListener('change', (event) => {
        const file = event.target.files[0];
        
        videoFile = file;
        processVideo(file);
    });

    // --- Submit (Giữ nguyên) ---
    submitBtn.addEventListener('click', (e) => {
        e.preventDefault(); 
        if (!videoFile || points.length !== 2) return;

        const formData = new FormData();
        formData.append('videoFile', videoFile);
        
        const lineData = {
            p1: points[0],
            p2: points[1]
        };
        formData.append('lineData', JSON.stringify(lineData));

        loadingSpinner.style.display = 'flex';
        submitBtn.disabled = true;

        fetch('/api/v1/process', {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { 
                    throw new Error(`Lỗi server: ${text}`); 
                });
            }
            return response.text(); 
        })
        .then(logJsonString => {
            console.log('Kết quả nhận diện:', logJsonString);
            
            // Lưu kết quả vào sessionStorage
            sessionStorage.setItem('violationLogs', logJsonString);
            
            // Tắt loading và chuyển qua trang kết quả
            loadingSpinner.style.display = 'none';
            submitBtn.disabled = false;
            
            console.log('Chuyển hướng đến results.html');
            window.location.href = '/html/results.html';
        })
        .catch(error => {
            alert(`Đã xảy ra lỗi: ${error.message}`);
            loadingSpinner.style.display = 'none';
            submitBtn.disabled = false;
        });
    });
});