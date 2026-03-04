package klikr.machine_learning.monitoring;

public record UDP_report(String server_uuid, String model_name, String image_path, double processing_time) {}
