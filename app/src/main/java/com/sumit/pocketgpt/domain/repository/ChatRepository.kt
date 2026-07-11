package com.sumit.pocketgpt.domain.repository

interface ChatRepository {

    fun getModelStatus(): String
}