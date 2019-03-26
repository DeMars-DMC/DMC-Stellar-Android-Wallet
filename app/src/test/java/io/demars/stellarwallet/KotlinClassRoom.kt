package io.demars.stellarwallet

import org.junit.Test

class KotlinClassRoom {
    @Test
    fun testLetElse(){
        val value : String? = null
        value?.let {
            assert(false)
        }?:run {
            assert(true)
        }
    }

    @Test
    fun testLetElse2(){
        val value : String? = "234"
        value?.let {
            assert(true)
        }?:run {
            assert(false)
        }
    }
}
