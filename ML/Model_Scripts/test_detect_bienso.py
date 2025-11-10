"""
Script test nhanh model phát hiện biển số
Chạy trực tiếp mà không cần tham số
"""

import cv2
from ultralytics import YOLO
import os

# ============ CẤU HÌNH ============
MODEL_PATH = "E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/ML/Model_Scripts/models/yolov8_detect_bienso.pt"
IMAGE_PATH = "C:/Users/Admin/Pictures/Screenshots/Screenshot 2025-11-10 120902.png"  # Thay đường dẫn ảnh test của bạn ở đây
# ==================================

print("=" * 60)
print("  TEST NHANH MODEL PHÁT HIỆN BIỂN SỐ")
print("=" * 60)

# Kiểm tra file
if not os.path.exists(MODEL_PATH):
    print(f"[ERROR] Model không tồn tại: {MODEL_PATH}")
    print("[INFO] Vui lòng kiểm tra đường dẫn model!")
    exit(1)

if not os.path.exists(IMAGE_PATH):
    print(f"[WARNING] Ảnh test không tồn tại: {IMAGE_PATH}")
    print("[INFO] Vui lòng cung cấp ảnh test!")
    print("[INFO] Bạn có thể:")
    print("  1. Đặt ảnh test tại:", IMAGE_PATH)
    print("  2. Hoặc sửa IMAGE_PATH trong file này")
    exit(1)

# Load model
print(f"\n[1] Đang load model...")
model = YOLO(MODEL_PATH)
print("[✓] Model loaded successfully!")

# Đọc ảnh
print(f"\n[2] Đang đọc ảnh test...")
image = cv2.imread(IMAGE_PATH)
print(f"[✓] Kích thước ảnh: {image.shape[1]}x{image.shape[0]} pixels")

# Chạy detection
print(f"\n[3] Đang chạy detection...")
results = model(image, verbose=False)

# Phân tích kết quả
print(f"\n[4] Kết quả:")
print("-" * 60)

if not results or len(results) == 0:
    print("[✗] Không có kết quả")
    exit(1)

result = results[0]

if result.boxes is None or len(result.boxes) == 0:
    print("[✗] KHÔNG PHÁT HIỆN ĐƯỢC BIỂN SỐ!")
    print("\nGợi ý:")
    print("  - Thử ảnh khác có xe rõ ràng hơn")
    print("  - Kiểm tra model có đúng không")
    print("  - Biển số có bị che khuất không")
else:
    print(f"[✓] PHÁT HIỆN ĐƯỢC {len(result.boxes)} BIỂN SỐ!")
    print()
    
    output_image = image.copy()
    
    for idx, box in enumerate(result.boxes):
        x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
        conf = box.conf[0].cpu().numpy()
        
        print(f"  Biển số #{idx + 1}:")
        print(f"    • Vị trí: x={int(x1)}, y={int(y1)}, w={int(x2-x1)}, h={int(y2-y1)}")
        print(f"    • Độ tin cậy: {conf*100:.1f}%")
        
        # Đánh giá chất lượng detection
        if conf > 0.7:
            quality = "Rất tốt ✓✓✓"
            color = (0, 255, 0)
        elif conf > 0.5:
            quality = "Tốt ✓✓"
            color = (0, 200, 100)
        elif conf > 0.3:
            quality = "Trung bình ✓"
            color = (0, 165, 255)
        else:
            quality = "Thấp ✗"
            color = (0, 0, 255)
        
        print(f"    • Chất lượng: {quality}")
        
        # Vẽ box
        cv2.rectangle(output_image, (int(x1), int(y1)), (int(x2), int(y2)), color, 3)
        cv2.putText(output_image, f"Plate {conf:.2f}", (int(x1), int(y1)-10),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)
        
        # Crop biển số
        plate_crop = image[int(y1):int(y2), int(x1):int(x2)]
        crop_name = f"plate_crop_{idx+1}.jpg"
        cv2.imwrite(crop_name, plate_crop)
        print(f"    • Đã lưu crop: {crop_name}")
        print()
    
    # Lưu kết quả
    output_name = "detection_result.jpg"
    cv2.imwrite(output_name, output_image)
    print(f"[✓] Đã lưu kết quả: {output_name}")
    
    # Hiển thị ảnh
    try:
        print("\n[5] Đang hiển thị kết quả...")
        print("    (Nhấn phím bất kỳ để đóng)")
        cv2.imshow("Plate Detection", output_image)
        cv2.waitKey(0)
        cv2.destroyAllWindows()
    except:
        print("[INFO] Không thể hiển thị (không có GUI)")

print("\n" + "=" * 60)
print("[DONE] Hoàn thành!")
print("=" * 60)
