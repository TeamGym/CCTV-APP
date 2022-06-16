package kr.ac.kpu.cctvmanager.datatype

data class CamInfo(val id: Int, val name: String, var rtspUrl: String? = null)