"""
Script Python xử lý video phát hiện vi phạm vượt vạch dừng
- Nhận 5 tham số từ Java qua command line
- Load 3 models YOLOv8
- Xử lý video: detect xe → track → kiểm tra vi phạm → detect biển số → OCR
- Trả kết quả JSON về Java qua stdout
"""

import cv2
import json
import os
import argparse
import sys
import uuid
import warnings

# Tắt tất cả warnings
warnings.filterwarnings("ignore")
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'  # Tắt log TensorFlow
os.environ['YOLO_VERBOSE'] = 'False'  # Tắt log YOLOv8

# Thiết lập encoding UTF-8 cho stdout (quan trọng cho Windows)
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# Import YOLOv8 (dùng cho Model 1 và Model 2)
from ultralytics import YOLO
import logging
logging.getLogger('ultralytics').setLevel(logging.ERROR)

# Import OCR (Model 3 - chọn 1 trong các options sau)
# TODO: Uncomment thư viện OCR mà nhóm đang dùng

# Option 1: PaddleOCR (Recommended)
# from paddleocr import PaddleOCR

# Option 2: EasyOCR
# import easyocr

# Option 3: Tesseract
# import pytesseract

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

        # --- Bước 2: Tải 3 models YOLOv8 ---
        # Model 1: Phát hiện xe trong video (object detection)
        print(f"[INFO] Đang tải model phát hiện xe: {args.object_model}", file=sys.stderr)
        model_object = YOLO(args.object_model, verbose=False)
        
        # Model 2: Phát hiện vùng biển số trong ảnh xe (plate detection)
        print(f"[INFO] Đang tải model phát hiện biển số: {args.plate_model}", file=sys.stderr)
        model_plate = YOLO(args.plate_model, verbose=False)
        
        # Model 3: OCR để đọc text từ ảnh biển số
        # TODO: Thay đổi cách load model OCR tùy theo thư viện đang dùng
        print(f"[INFO] Đang tải model OCR: {args.ocr_model}", file=sys.stderr)
        model_ocr = YOLO(args.ocr_model, verbose=False)

        # --- Bước 3: Đọc config vạch dừng ---
        with open(args.config, 'r', encoding='utf-8') as f:
            line_config = json.load(f)
            p1 = (line_config['p1']['x'], line_config['p1']['y'])
            p2 = (line_config['p2']['x'], line_config['p2']['y'])
            print(f"[INFO] Vạch dừng: P1={p1}, P2={p2}", file=sys.stderr)

        # --- Bước 4: Mở video ---
        print(f"[DEBUG] Đang mở video: {args.video}", file=sys.stderr)
        cap = cv2.VideoCapture(args.video)
        if not cap.isOpened():
            raise Exception(f"Không thể mở video: {args.video}")
        
        print(f"[DEBUG] ✅ Video đã mở thành công", file=sys.stderr)

        try:
            violation_logs = []  # Danh sách kết quả vi phạm
            frame_count = 0
            fps = cap.get(cv2.CAP_PROP_FPS) or 30  # FPS của video (mặc định 30)
            
            print(f"[INFO] Bắt đầu xử lý video... (FPS={fps})", file=sys.stderr)
            
            # --- Bước 5: Xử lý từng frame ---
            tracked_objects = {}  # Dict để track xe theo ID (tránh log trùng)
            
            while cap.isOpened():
                ret, frame = cap.read()
                if not ret:
                    print(f"[DEBUG] Đã đọc hết video tại frame {frame_count}", file=sys.stderr)
                    break
                
                frame_count += 1
                
                if frame_count % 30 == 0:  # Log mỗi 30 frames
                    print(f"[DEBUG] Đang xử lý frame {frame_count}...", file=sys.stderr)
                
                # 5a. Detect và track xe
                results = model_object.track(frame, persist=True, verbose=False)
                
                if frame_count % 30 == 0:
                    print(f"[DEBUG] Phát hiện {len(results)} kết quả từ model_object", file=sys.stderr)
                
                for result in results:
                    if result.boxes is None or len(result.boxes) == 0:
                        if frame_count % 30 == 0:
                            print(f"[DEBUG] Frame {frame_count}: Không có boxes", file=sys.stderr)
                        continue
                    
                    if frame_count % 30 == 0:
                        print(f"[DEBUG] Frame {frame_count}: Có {len(result.boxes)} boxes", file=sys.stderr)
                        
                    for box in result.boxes:
                        # Lấy thông tin xe
                        track_id = int(box.id[0]) if box.id is not None and len(box.id) > 0 else None
                        cls = int(box.cls[0]) if len(box.cls) > 0 else 0
                        conf = float(box.conf[0]) if len(box.conf) > 0 else 0.0
                        x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                        
                        # 5b. Kiểm tra vượt vạch dừng
                        bottom_center = ((x1 + x2) / 2, y2)  # Điểm dưới giữa xe
                        
                        if is_crossing_line(bottom_center, p1, p2):
                            # Xe vi phạm! Kiểm tra đã log chưa
                            print(f"[DEBUG] ⚠️ Frame {frame_count}: Xe vượt vạch! track_id={track_id}", file=sys.stderr)
                            if track_id is None or track_id not in tracked_objects:
                                # 5c. Crop vùng xe để detect biển số
                                print(f"[DEBUG] Đang crop vùng xe: ({int(x1)},{int(y1)}) -> ({int(x2)},{int(y2)})", file=sys.stderr)
                                car_crop = frame[int(y1):int(y2), int(x1):int(x2)]
                                
                                # 5d. Detect vùng biển số trong ảnh xe (Model 2)
                                print(f"[DEBUG] Đang detect biển số...", file=sys.stderr)
                                plate_results = model_plate(car_crop, verbose=False)
                                print(f"[DEBUG] Plate results: {len(plate_results)} kết quả", file=sys.stderr)
                                
                                license_plate = "Không xác định"
                                if len(plate_results) > 0 and plate_results[0].boxes is not None and len(plate_results[0].boxes) > 0:
                                    print(f"[DEBUG] ✅ Tìm thấy biển số trong ảnh xe!", file=sys.stderr)
                                    # Lấy bounding box của biển số (lấy detection đầu tiên)
                                    plate_box = plate_results[0].boxes[0].xyxy[0].cpu().numpy()
                                    px1, py1, px2, py2 = plate_box
                                    
                                    # Crop vùng biển số
                                    plate_crop = car_crop[int(py1):int(py2), int(px1):int(px2)]
                                    
                                    # 5e. OCR để đọc text biển số (Model 3)
                                    print(f"[DEBUG] Đang OCR biển số...", file=sys.stderr)
                                    license_plate = ocr_read_plate(model_ocr, plate_crop)
                                    print(f"[DEBUG] ✅ OCR kết quả: {license_plate}", file=sys.stderr)
                                else:
                                    print(f"[DEBUG] ❌ Không tìm thấy biển số trong ảnh xe", file=sys.stderr)
                                
                                # 5f. Lưu ảnh bằng chứng
                                print(f"[DEBUG] Đang lưu ảnh bằng chứng...", file=sys.stderr)
                                image_name = f"violation_{uuid.uuid4().hex}.jpg"
                                save_path = os.path.join(STATIC_IMAGE_SAVE_PATH, image_name)
                                
                                # Vẽ bounding box và text lên frame
                                cv2.rectangle(frame, (int(x1), int(y1)), (int(x2), int(y2)), (0, 0, 255), 2)
                                cv2.putText(frame, license_plate, (int(x1), int(y1)-10), 
                                           cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 0, 255), 2)
                                cv2.imwrite(save_path, frame)
                                
                                # 5g. Log vi phạm
                                video_time = frame_count / fps  # Thời gian trong video (giây)
                                minutes = int(video_time // 60)
                                seconds = int(video_time % 60)
                                
                                violation_logs.append({
                                    "frame": frame_count,
                                    "video_time": f"{minutes:02d}:{seconds:02d}",
                                    "license_plate": license_plate,
                                    "evidence_url": f"/violation_images/{image_name}",
                                    "confidence": round(conf, 2)
                                })
                                
                                # Đánh dấu đã log (nếu có track_id)
                                if track_id is not None:
                                    tracked_objects[track_id] = True
                                    
                                print(f"[INFO] Vi phạm frame {frame_count}: {license_plate}", file=sys.stderr)
            
            print(f"[INFO] Hoàn thành xử lý. Tổng số vi phạm: {len(violation_logs)}", file=sys.stderr)
            
        finally:
            # Đảm bảo luôn release video capture
            cap.release()
        
        # --- Bước 6: Trả kết quả JSON về Java qua stdout ---
        # QUAN TRỌNG: Chỉ print JSON ra stdout, các log khác dùng stderr
        result_json = json.dumps(violation_logs, ensure_ascii=False, indent=2)
        print(result_json)
        sys.stdout.flush()

    except Exception as e:
        # Nếu có lỗi, trả JSON error về Java
        error_log = {
            "error": True, 
            "message": str(e),
            "type": type(e).__name__
        }
        print(json.dumps(error_log, ensure_ascii=False), file=sys.stderr)
        sys.exit(1)


# Helper functions
def is_crossing_line(point, p1, p2):
    """
    Kiểm tra điểm có vượt qua đường thẳng p1-p2 không
    Sử dụng công thức: (y - y1) * (x2 - x1) - (y2 - y1) * (x - x1)
    - Nếu > 0: Điểm bên trên đường thẳng
    - Nếu < 0: Điểm bên dưới đường thẳng
    - Nếu = 0: Điểm nằm trên đường thẳng
    
    Args:
        point: tuple (x, y) - Tọa độ điểm cần kiểm tra
        p1: tuple (x1, y1) - Điểm đầu của đường thẳng
        p2: tuple (x2, y2) - Điểm cuối của đường thẳng
    
    Returns:
        bool: True nếu điểm vượt qua đường thẳng (y > đường thẳng)
    """
    x, y = point
    x1, y1 = p1
    x2, y2 = p2
    
    # Tính vị trí tương đối
    cross_product = (y - y1) * (x2 - x1) - (y2 - y1) * (x - x1)
    
    # TODO: Điều chỉnh logic tùy theo hướng vạch dừng của bạn
    # Hiện tại: điểm ở phía dưới đường thẳng = vượt vạch
    return cross_product < 0


def ocr_read_plate(model, plate_image):
    """
    Đọc biển số từ ảnh crop sử dụng OCR
    
    Args:
        model: OCR model đã load (PaddleOCR/EasyOCR/Tesseract/YOLO)
        plate_image: numpy array - Ảnh crop biển số
    
    Returns:
        str: Biển số đọc được (VD: "29A-12345")
    """
    try:
        # ==================== OPTION 1: PaddleOCR ====================
        # Uncomment nếu dùng PaddleOCR
        # result = model.ocr(plate_image, cls=True)
        # if result and result[0]:
        #     # result[0] = [([[x1,y1], [x2,y2], [x3,y3], [x4,y4]], (text, confidence))]
        #     text = result[0][0][1][0]  # Lấy text
        #     text = text.replace(' ', '').replace('-', '')  # Làm sạch
        #     return text if text else "Không xác định"
        # return "Không xác định"
        
        # ==================== OPTION 2: EasyOCR ====================
        # Uncomment nếu dùng EasyOCR
        # result = model.readtext(plate_image)
        # if result:
        #     # result = [(bbox, text, confidence)]
        #     text = result[0][1]  # Lấy text đầu tiên
        #     text = text.replace(' ', '').replace('-', '')  # Làm sạch
        #     return text if text else "Không xác định"
        # return "Không xác định"
        
        # ==================== OPTION 3: Tesseract ====================
        # Uncomment nếu dùng Tesseract
        # import pytesseract
        # text = pytesseract.image_to_string(plate_image, config='--psm 7 -c tessedit_char_whitelist=0123456789ABCDEFGHKLMNPRSTUVXYZ')
        # text = text.strip().replace(' ', '').replace('\n', '')
        # return text if text else "Không xác định"
        
        # ==================== OPTION 4: YOLOv8 (Character Detection) ====================
        # Nếu OCR là YOLOv8 detect từng ký tự - GIỮ PHẦN NÀY nếu dùng YOLO
        results = model(plate_image, verbose=False)
        
        if len(results) > 0 and results[0].boxes is not None and len(results[0].boxes) > 0:
            detected_chars = []
            
            for box in results[0].boxes:
                if len(box.cls) == 0 or len(box.xyxy) == 0:
                    continue
                cls_id = int(box.cls[0])
                char = results[0].names[cls_id]  # Ký tự: '0'-'9', 'A'-'Z', etc.
                x_center = (box.xyxy[0][0] + box.xyxy[0][2]) / 2
                detected_chars.append((x_center.item(), char))
            
            # Sort theo vị trí x (trái → phải)
            detected_chars.sort(key=lambda x: x[0])
            license_plate = ''.join([char for _, char in detected_chars])
            
            return license_plate if license_plate else "Không xác định"
        
        return "Không xác định"
        
    except Exception as e:
        print(f"[ERROR] OCR failed: {e}", file=sys.stderr)
        return "Không xác định"


if __name__ == "__main__":
    main()