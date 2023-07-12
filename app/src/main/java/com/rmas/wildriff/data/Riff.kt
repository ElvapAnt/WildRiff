package com.rmas.wildriff.data

import org.osmdroid.util.GeoPoint

data class Riff(
    var name:String?,
    var pitch:String?,
    var tonality:String?,
    var key:String?,
    var userId:String?,
    var riffId:String?,
    var latitude: Double,
    var longitude:Double,
    var avgGrade:Float,
    var grades:Any?
){
    // No-argument constructor required by Firebase
    constructor() : this("", "", "", "","","",0.0,0.0,0.0f, null)
    val gradesList: List<Grade>
        get() = when (grades) {
            is List<*> -> (grades as List<*>).filterIsInstance<Grade>()
            is Map<*, *> -> (grades as Map<*, *>).values.filterIsInstance<Grade>()
            else -> emptyList()
        }
}
