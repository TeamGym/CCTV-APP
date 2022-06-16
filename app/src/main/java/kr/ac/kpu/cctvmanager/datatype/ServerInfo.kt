package kr.ac.kpu.cctvmanager.datatype

data class ServerInfo(val httpPort: Int, val udpReceiverPortRange: List<Int>, val tcpPort: Int, val cameraList: List<CamInfo>, var address: String? = null)