package kr.ac.kpu.cctvmanager.datatype

data class ServerInfo(val httpPort: Int, val rtspPlayPort: Int, val rtspRecordPort: Int, val tcpPort: Int, val cameraList: List<CamInfo>, var address: String? = null)