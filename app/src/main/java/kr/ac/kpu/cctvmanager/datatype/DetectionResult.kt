package kr.ac.kpu.cctvmanager.datatype

data class DetectionResult(val timestamp: Double, val boxes: List<DetectionBox>)
