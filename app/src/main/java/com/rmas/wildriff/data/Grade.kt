package com.rmas.wildriff.data

data class Grade(
    var value : Float?,
    var riffId : String?,
    var userId : String?
    ){
    // No-argument constructor required by Firebase
    constructor() : this(0.0f, "","")
}
