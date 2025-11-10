package com.technicjelle.bluemapofflineplayermarkers.struct

import java.util.*

data class OfflinePlayer(
    val uuid: UUID,
    val name: String,
    val lastTimeOnline: Long,
    val position: List<Double>,
    val dimension: String,
    val gameMode: Int
)