package com.evrenhouse.esteh.viewmodel

import com.evrenhouse.esteh.data.remote.dto.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogTest {

    @Test
    fun `catalog maps web assets`() {
        val catalog = menuCatalog(Settings.DEFAULT)
        assertEquals(
            listOf("es-teh.svg", "lemon-tea.svg"),
            catalog.map { it.first.asset },
        )
        assertEquals(listOf(10_000, 11_000), catalog.map { it.second })
    }

    @Test
    fun `asset url joins base`() {
        assertEquals(
            "https://esteh.sanxmon.my.id/assets/es-teh.svg",
            assetUrl("https://esteh.sanxmon.my.id", "es-teh.svg"),
        )
        assertEquals(
            "http://10.0.2.2:3006/assets/lemon-tea.svg",
            assetUrl("http://10.0.2.2:3006/", "/lemon-tea.svg"),
        )
    }
}
