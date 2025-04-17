package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class NotifyRegisterPhone(val username: String, val phoneId: String)
