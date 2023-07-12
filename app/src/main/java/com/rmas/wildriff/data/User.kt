package com.rmas.wildriff.data

import java.io.File
import java.io.Serializable

data class User(
    var userId : String,
    var firstName:String,
    var lastName:String,
    var email: String,
    var username:String,
    var password:String,
    var phoneNumber:String,
    var profileImageUrl:String,
    var score:Float
){
    // No-argument constructor required by Firebase
    constructor() : this("","", "", "", "", "", "", "",0.0f)
}
