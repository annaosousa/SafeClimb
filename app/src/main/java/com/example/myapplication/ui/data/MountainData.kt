package com.example.myapplication.ui.data

object MountainData {
    val mountains = mapOf(
        "Pico do Paraná" to Pair(-25.2427, -48.8395),
        "Pico do Caratuva" to Pair(-25.2042, -48.7345),
        "Morro do Araçatuba" to Pair(-25.1961, -48.8731),
        "Pico Marumbi" to Pair(-25.1805, -48.9443),
        "Morro do Anhangava" to Pair(-25.4167, -48.9481),
        "Morro do Canal" to Pair(-25.2633, -48.7647)
    )

    // Coordenadas das torres para cada montanha
    val towerLocations = mapOf(
        "Pico do Paraná" to listOf(
            Pair(-25.2425, -48.8420),
            Pair(-25.2430, -48.8410)
        ),
        "Pico do Caratuva" to listOf(
            Pair(-25.2022, -48.7245),
            Pair(-25.2142, -48.7345)
        ),
        "Morro do Araçatuba" to listOf(
            Pair(-25.1971, -48.8831),
            Pair(-25.1861, -48.8701)
        ),
        "ico Marumbi" to listOf(
            Pair(-25.1825, -48.9433),
            Pair(-25.1895, -48.9403)
        ),
        "Morro do Anhangava" to listOf(
            Pair(-25.4147, -48.9487),
            Pair(-25.4107, -48.9471)
        ),
        "Morro do Canal" to listOf(
            Pair(-25.2673, -48.7627),
            Pair(-25.2663, -48.7677)
        )
    )
}
