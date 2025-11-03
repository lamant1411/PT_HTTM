import cv2
import json
import argparse
import sys
import base64

import io
# sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
# sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# Import YOLOv8
from ultralytics import YOLO
import logging
logging.getLogger('ultralytics').setLevel(logging.ERROR)

# Đường dẫn tuyệt đối đến violation_images
STATIC_IMAGE_SAVE_PATH = "E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/Client/src/main/resources/static/violation_images/"

def main():
    try:
        # --- Đọc 5 tham số (đường dẫn tới video, vạch dừng và 3 model xử lý bài toán) ---
        parser = argparse.ArgumentParser(description="Xử lý video phát hiện vi phạm.")
        parser.add_argument("--video", required=True, help="Đường dẫn file video tạm")
        parser.add_argument("--config", required=True, help="Đường dẫn file config vạch dừng")
        parser.add_argument("--object_model", required=True, help="Đường dẫn model phát hiện vật thể")
        parser.add_argument("--plate_model", required=True, help="Đường dẫn model phát hiện biển số")
        parser.add_argument("--ocr_model", required=True, help="Đường dẫn model đọc biển số")
        
        args = parser.parse_args()

        # --- load 3 model ---
        # Nhận diện đối tượng (phương tiện)
        print(f"(INFO) Loading vehicle detection model: {args.object_model}", file=sys.stderr)
        model_object = YOLO(args.object_model, verbose=False)

        # Nhận diện biển số
        print(f"(INFO) Loading plate detection model: {args.plate_model}", file=sys.stderr)
        model_plate = YOLO(args.plate_model, verbose=False)

        # Đọc biển số (OCR)
        print(f"(INFO) Loading OCR model: {args.ocr_model}", file=sys.stderr)
        model_ocr = YOLO(args.ocr_model, verbose=False)

        # --- Nhận vạch dừng ---
        with open(args.config, 'r', encoding='utf-8') as f:
            line_config = json.load(f)
            p1 = (line_config['p1']['x'], line_config['p1']['y'])
            p2 = (line_config['p2']['x'], line_config['p2']['y'])
            print(f"(INFO) Stop line: P1={p1}, P2={p2}", file=sys.stderr)

        # --- Mở video ---
        print(f"(DEBUG) Opening video: {args.video}", file=sys.stderr)
        cap = cv2.VideoCapture(args.video)
        if not cap.isOpened():
            raise Exception(f"Cannot open video: {args.video}")

        print(f"(DEBUG) Video opened successfully", file=sys.stderr)

        try:
            violation_logs = [] # Lưu kết quả vi phạm
            frame_count = 0
            fps = cap.get(cv2.CAP_PROP_FPS) 
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            print(f"(INFO) Start processing video... (FPS={fps}, Total frames: {total_frames})", file=sys.stderr)

            # --- Xử lý từng frame ---
            tracked_objects = {}  # gán ID cho các xe (để tránh log trùng)
            previous_positions = {}  # Lưu trữ vị trí trước đó: {unique_key: (is_above_line, frame)}

            while cap.isOpened():
                ret, frame = cap.read()
                if not ret:
                    print(f"(DEBUG) End of video at frame {frame_count}", file=sys.stderr)
                    break
                
                frame_count += 1
                
                # Nhận diện và tracking phương tiện
                try:
                    results = model_object.track(frame, persist=True, verbose=False, tracker="bytetrack.yaml")
                except:
                    results = model_object(frame, verbose=False)

                for result in results:
                    if result.boxes is None or len(result.boxes) == 0:
                        continue

                    for box in result.boxes:
                        # Lấy thông tin xe
                        track_id = None
                        if hasattr(box, 'id') and box.id is not None:
                            try:
                                if len(box.id) > 0:
                                    track_id = int(box.id[0])
                            except:
                                track_id = None
                        
                        cls = int(box.cls[0]) if len(box.cls) > 0 else 0
                        conf = float(box.conf[0]) if len(box.conf) > 0 else 0.0
                        x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                        
                        # Kiểm tra vượt vạch dừng
                        bottom_center = ((x1 + x2) / 2, y2)  # Điểm dưới giữa xe
                        
                        # Tạo unique key để track xe
                        if track_id is not None:
                            unique_key = f"id_{track_id}"
                        else:
                            # Nếu không có track_id, dùng vị trí (làm tròn 30px để group chặt hơn)
                            unique_key = f"pos_{int(bottom_center[0]//30)}_{int(bottom_center[1]//30)}"
                        
                        # Kiểm tra vị trí hiện tại so với vạch
                        is_below_line = is_crossing_line(bottom_center, p1, p2)
                        
                        # Lấy trạng thái trước đó (nếu có)
                        if unique_key in previous_positions:
                            was_above_line, last_frame = previous_positions[unique_key]
                        else:
                            was_above_line = not is_below_line  # Giả sử xe bắt đầu ở phía trên
                            last_frame = 0
                        
                        # CẬP NHẬT vị trí hiện tại
                        previous_positions[unique_key] = (not is_below_line, frame_count)
                        
                        if was_above_line and is_below_line and unique_key not in tracked_objects:
                            # Violation detected!
                            print(f"(VIOLATION) Detected at frame {frame_count}: key={unique_key}", file=sys.stderr)
                            
                            car_crop = frame[int(y1):int(y2), int(x1):int(x2)]
                            
                            plate_results = model_plate(car_crop, verbose=False)

                            license_plate = "Unknown"
                            if len(plate_results) > 0 and plate_results[0].boxes is not None and len(plate_results[0].boxes) > 0:
                                # Lấy bounding box của biển số (lấy detection đầu tiên)
                                plate_box = plate_results[0].boxes[0].xyxy[0].cpu().numpy()
                                px1, py1, px2, py2 = plate_box
                                
                                plate_crop = car_crop[int(py1):int(py2), int(px1):int(px2)]
                                
                                # 5e. OCR to read license plate text (Model 3)
                                license_plate = ocr_read_plate(model_ocr, plate_crop)

                            # 5f. Create evidence image (NOT SAVED YET - only encode to base64)

                                # Clone frame for drawing (don't modify original)
                                evidence_frame = frame.copy()
                                
                                # Draw bounding box and license plate text
                                cv2.rectangle(evidence_frame, (int(x1), int(y1)), (int(x2), int(y2)), (0, 0, 255), 2)
                                cv2.putText(evidence_frame, license_plate, (int(x1), int(y1)-10), 
                                           cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 0, 255), 2)
                                
                                # Encode image to base64 (for sending to frontend)
                                encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 60]
                                _, buffer = cv2.imencode('.jpg', evidence_frame, encode_param)
                                image_base64 = base64.b64encode(buffer).decode('utf-8')
                                
                                # 5g. Log violation
                                video_time = frame_count / fps  # Time in video (seconds)
                                minutes = int(video_time // 60)
                                seconds = int(video_time % 60)
                                
                                violation_logs.append({
                                    "frame": frame_count,
                                    "video_time": f"{minutes:02d}:{seconds:02d}",
                                    "license_plate": license_plate,
                                    "evidence_image_base64": f"data:image/jpeg;base64,{image_base64}",  # Base64 for display
                                    "confidence": round(conf, 2)
                                })
                                
                            # Mark as logged
                            tracked_objects[unique_key] = True
                            print(f"(VIOLATION) Logged: {license_plate}", file=sys.stderr)

            print(f"(INFO) Processing complete. Total violations: {len(violation_logs)}", file=sys.stderr)

        finally:
            # Always release video capture
            cap.release()
        
        # --- Step 6: Return JSON result to Java via stdout ---
        # IMPORTANT: Only print JSON to stdout, use stderr for logs
        result_json = json.dumps(violation_logs, ensure_ascii=False, indent=2)
        print(result_json)
        sys.stdout.flush()

    except Exception as e:
        # If error occurs, return JSON error to Java
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
    Sử dụng công thức: a = (y - y1) * (x2 - x1) - (y2 - y1) * (x - x1)
    - Nếu a > 0: Điểm bên trên đường thẳng
    - Nếu a < 0: Điểm bên dưới đường thẳng
    - Nếu a = 0: Điểm nằm trên đường thẳng
    
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
    đọc biển số xe bằng OCR

    Args:
        model: model OCR đã load (PaddleOCR/EasyOCR/Tesseract/YOLO)
        plate_image: numpy array - Cropped plate image

    Returns:
        str: License plate text (e.g., "29A-12345")
    """
    try:
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
            
            # Sort by x position (left to right)
            detected_chars.sort(key=lambda x: x[0])
            license_plate = ''.join([char for _, char in detected_chars])
            
            return license_plate if license_plate else "Unknown"
        
        return "Unknown"
        
    except Exception as e:
        print(f"[ERROR] OCR failed: {e}", file=sys.stderr)
        return "Unknown"


if __name__ == "__main__":
    main()