package com.lin0721.linmusic.core

import org.junit.Test

class CheckClientsTest {
    @Test
    fun listClients() {
        val clazz = Class.forName("com.metrolist.innertubex.models.YouTubeClient")
        println("=== YouTubeClient fields ===")
        for (field in clazz.fields) {
            println("${field.name}: ${field.type.name}")
        }
        for (field in clazz.declaredFields) {
            println("declared: ${field.name}: ${field.type.name}")
        }
    }
}
