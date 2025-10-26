# BTL Project Skeleton

This workspace contains a three-part skeleton:

- `Client/` - Spring Boot application (port 8080) that accepts video uploads and calls a Python script via ProcessBuilder.
- `ML/` - Spring Boot admin app (port 8081) for managing models and subproblems (simple skeleton).
- `Model_Scripts/` - Python scripts and model artifacts (put your yolov8n.pt here). `process.py` is the demo script called by the Client.

Quick start notes:

1. Build the Java modules: open each module folder (`Client` and `ML`) and run `mvn spring-boot:run` (requires Maven and JDK 11+).
2. Install Python deps for `Model_Scripts` (use virtualenv): `pip install -r requirements.txt`.
3. Ensure `python` on PATH points to a Python 3 interpreter. If not, adjust `model.scripts.path` or `ProcessingService` invocation.

Security / model files: keep heavy model weights out of Git; put them into `Model_Scripts/models/` and add download/install steps in CI.
