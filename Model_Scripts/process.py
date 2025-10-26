import cv2
import json
import os
import argparse  # Để đọc tham số
import sys       # Để flush output
import time      # Để giả lập thời gian xử lý
import uuid      # Để tạo tên file duy nhất

# Thiết lập encoding UTF-8 cho stdout
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# --- CẤU HÌNH QUAN TRỌNG ---
# Đường dẫn TUYỆT ĐỐI đến thư mục static của CLIENT
# Python phải biết nơi để lưu ảnh bằng chứng
# Sửa lại đường dẫn này cho đúng với máy của bạn
# (Đây là thư mục mà Spring Boot có thể phục vụ web)
STATIC_IMAGE_SAVE_PATH = "E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/Client/src/main/resources/static/violation_images/"

def main():
    try:
        # Tạo thư mục lưu ảnh nếu chưa tồn tại
        os.makedirs(STATIC_IMAGE_SAVE_PATH, exist_ok=True)
        
        # --- Bước 1: Đọc 5 tham số từ Java ---
        parser = argparse.ArgumentParser(description="Xử lý video phát hiện vi phạm.")
        parser.add_argument("--video", required=True, help="Đường dẫn file video tạm")
        parser.add_argument("--config", required=True, help="Đường dẫn file config vạch dừng")
        parser.add_argument("--object_model", required=True, help="Đường dẫn model phát hiện vật thể")
        parser.add_argument("--plate_model", required=True, help="Đường dẫn model phát hiện biển số")
        parser.add_argument("--ocr_model", required=True, help="Đường dẫn model đọc biển số")
        
        args = parser.parse_args()

        # --- Bước 2: Tải model (Giả lập) ---
        # print("Đang tải model vật thể từ:", args.object_model)
        # model_object = YOLO(args.object_model) # Code thật
        
        # print("Đang tải model biển số từ:", args.plate_model)
        # model_plate = YOLO(args.plate_model) # Code thật
        
        # print("Đang tải model OCR từ:", args.ocr_model)
        # model_ocr = load_ocr_model(args.ocr_model) # Code thật

        # --- Bước 3: Đọc video và config ---
        with open(args.config, 'r') as f:
            line_config = json.load(f)
            # p1 = (line_config['p1']['x'], line_config['p1']['y'])
            # p2 = (line_config['p2']['x'], line_config['p2']['y'])

        cap = cv2.VideoCapture(args.video)
        if not cap.isOpened():
            raise Exception(f"Không thể mở video: {args.video}")

        violation_logs = [] # Nơi lưu kết quả
        frame_count = 0
        
        # --- Bước 4: Lặp qua video và xử lý (Phần logic AI của nhóm bạn) ---
        
        # (Đây là phần code thật của bạn, ví dụ)
        # while cap.isOpened():
        #     ret, frame = cap.read()
        #     if not ret:
        #         break
        #     frame_count += 1
            
        #     # 1. Chạy model vật thể + tracking
        #     # results = model_object.track(frame, persist=True)
        #     # tracked_cars = ...
            
        #     # 2. Kiểm tra vi phạm (vượt vạch dừng)
        #     # for car in tracked_cars:
        #     #     if is_violating(car, p1, p2):
        #     #         ... (xử lý vi phạm)
        
        # --- (START SIMULATION) ---
        # Simulate processing with 2 violations
        time.sleep(2) # Simulate processing time
        
        # Simulate reading frame 100 and detecting violation
        ret, frame_100 = cap.read()
        if ret:
            # 3. Run plate detection model (simulated)
            license_plate_1 = "29A-123.45"
            
            # 4. Save evidence image
            image_name_1 = f"violation_{uuid.uuid4()}.jpg"
            save_path_1 = os.path.join(STATIC_IMAGE_SAVE_PATH, image_name_1)
            cv2.imwrite(save_path_1, frame_100)
            
            # 5. Log violation
            violation_logs.append({
                "frame": 100,
                "license_plate": license_plate_1,
                "evidence_url": f"/violation_images/{image_name_1}"
            })

        time.sleep(1) # Simulate more processing
        
        # Simulate detecting second violation
        cap.set(cv2.CAP_PROP_POS_FRAMES, 200)
        ret, frame_200 = cap.read()
        if ret:
            license_plate_2 = "51G-888.88"
            image_name_2 = f"violation_{uuid.uuid4()}.jpg"
            save_path_2 = os.path.join(STATIC_IMAGE_SAVE_PATH, image_name_2)
            cv2.imwrite(save_path_2, frame_200)
            
            violation_logs.append({
                "frame": 200,
                "license_plate": license_plate_2,
                "evidence_url": f"/violation_images/{image_name_2}"
            })
        
        # --- (END SIMULATION) ---

        cap.release()
        
        # --- Step 5: Return results to Java ---
        # Print JSON to stdout
        print(json.dumps(violation_logs, indent=2, ensure_ascii=False))
        sys.stdout.flush()

    except Exception as e:
        # If error occurs, print error message (Java will read it)
        error_log = {"error": True, "message": str(e)}
        print(json.dumps(error_log, ensure_ascii=False))
        sys.exit(1)

if __name__ == "__main__":
    main()