#!/usr/bin/env bash
MONITOR_UDP_PORT=$1

source ~/.klikr/venv/bin/activate

FACENET_EMBEDDINGS_PORTS=(8020 8021)
for PORT in "${FACENET_EMBEDDINGS_PORTS[@]}"; do
    LOGFILE="facenet_${PORT}.txt"
    python3 -c "import FaceNet_embeddings_server;
FaceNet_embeddings_server.run_server(${PORT}, ${MONITOR_UDP_PORT})"  >"$LOGFILE" 2>&1 &
done


FACE_DETECTION_PORTS=(8040 8041 8042 8043 8044 8045 8046 8047 8048 8049)
FACE_DETECTOR_ID="'MTCNN'"
for PORT in "${FACE_DETECTION_PORTS[@]}"; do
    LOGFILE="MTCNN_${PORT}.txt"
    python3 -c "import MTCNN_face_detection_server;
MTCNN_face_detection_server.run_server(${PORT},${FACE_DETECTOR_ID}, ${MONITOR_UDP_PORT})" >"$LOGFILE" 2>&1 &
done

FACE_DETECTION_PORTS=(8080 8081)
FACE_DETECTOR_ID="'tree.xml'"
for PORT in "${FACE_DETECTION_PORTS[@]}"; do
    LOGFILE="Haar_${PORT}.txt"
    python3 -c "import Haar_face_detection_server;
Haar_face_detection_server.run_server(${PORT},${FACE_DETECTOR_ID}, ${MONITOR_UDP_PORT})" >"$LOGFILE" 2>&1 &
done

FACE_DETECTION_PORTS=(8090 8091)
FACE_DETECTOR_ID="'default.xml'"
for PORT in "${FACE_DETECTION_PORTS[@]}"; do
    LOGFILE="Haar_${PORT}.txt"
    python3 -c "import Haar_face_detection_server;
Haar_face_detection_server.run_server(${PORT},${FACE_DETECTOR_ID}, ${MONITOR_UDP_PORT})" >"$LOGFILE" 2>&1 &
done

FACE_DETECTION_PORTS=(8100 8101)
FACE_DETECTOR_ID="'alt1.xml'"
for PORT in "${FACE_DETECTION_PORTS[@]}"; do
    LOGFILE="Haar_${PORT}.txt"
    python3 -c "import Haar_face_detection_server;
Haar_face_detection_server.run_server(${PORT},${FACE_DETECTOR_ID}, ${MONITOR_UDP_PORT})" >"$LOGFILE" 2>&1  &
done

FACE_DETECTION_PORTS=(8110 8111)
FACE_DETECTOR_ID="'alt2.xml'"
for PORT in "${FACE_DETECTION_PORTS[@]}"; do
    LOGFILE="Haar_${PORT}.txt"
    python3 -c "import Haar_face_detection_server;
Haar_face_detection_server.run_server(${PORT},${FACE_DETECTOR_ID}, ${MONITOR_UDP_PORT})" >"$LOGFILE" 2>&1 &
done
