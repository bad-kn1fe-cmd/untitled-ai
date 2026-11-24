package com.example.util

import androidx.compose.runtime.mutableStateMapOf

data class PetStateData(
    val index: Int,
    val variant: String,
    val xPercent: Float,
    val yPx: Float
)

object PetSocialMediator {
    val activePetStates = mutableStateMapOf<Int, PetStateData>()

    fun updatePetState(index: Int, variant: String, xPercent: Float, yPx: Float) {
        activePetStates[index] = PetStateData(index, variant, xPercent, yPx)
    }

    fun removePetState(index: Int) {
        activePetStates.remove(index)
    }

    fun getNearbyPet(myIndex: Int, myXPercent: Float, myYPx: Float): PetStateData? {
        val candidates = activePetStates.values.filter { it.index != myIndex }
        for (other in candidates) {
            val xDist = kotlin.math.abs(other.xPercent - myXPercent)
            val yDist = kotlin.math.abs(other.yPx - myYPx)
            if (xDist < 0.14f && yDist < 90f) {
                return other
            }
        }
        return null
    }
}
