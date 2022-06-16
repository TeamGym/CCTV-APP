package kr.ac.kpu.cctvmanager.datatype

data class DetectionBox(val x: Int, val y: Int, val width: Int, val height: Int, val confidence: Double, val classID: Int, val label: String)
