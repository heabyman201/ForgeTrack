package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import kotlin.math.sqrt

data class Vec3 ( val x: Double, val y: Double, val z: Double)

fun magnitude(v: Vec3): Double {
    return sqrt(v.x * v.x + v.y * v.y + v.z * v.z)

}
fun normalise(v: Vec3): Vec3 {
    val mag = magnitude(v)
    if (mag == 0.0) return Vec3(0.0,0.0,0.0)
    return Vec3(v.x / mag, v.y / mag, v.z / mag)

}

fun dotProduct(a: Vec3, b: Vec3) : Double{
    return a.x * b.x + a.y * b.y + a.z * b.z

}

fun similarity (a: Vec3, b: Vec3) : Double {
    return dotProduct(normalise(a), normalise(b))
}
fun averageSimilarity(vectors: List<Vec3>): Double {
    if (vectors.size < 2) return 0.0

    var sum = 0.0
    for (i in 1 until vectors.size) {
        sum += similarity(vectors[i], vectors[i - 1])
    }

    return sum / (vectors.size - 1)
}


//fun gradientAss(){
//            var x = 2.0
//            val learningRate = 0.01
//            repeat(50){
//                val gradient  = 4 * x.pow(3) - 9 * x.pow(2)
//                x -= learningRate * gradient
//                println("x = $x")
//            }
//        }
//        gradientAss()