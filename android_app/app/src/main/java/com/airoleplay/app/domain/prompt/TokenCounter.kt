package com.airoleplay.app.domain.prompt

import com.airoleplay.app.data.remote.api.KoboldCPPClient

class TokenCounter @javax.inject.Inject constructor() {
    private var koboldCPPClient: KoboldCPPClient? = null

    fun setClient(client: KoboldCPPClient) {
        this.koboldCPPClient = client
    }
    /**
     * Heuristic: 1 token ≈ 4 characters
     */
    fun estimateTokens(text: String): Int {
        return (text.length / 4.0).toInt()
    }

    /**
     * Exact count via KoboldCPP /api/extra/tokencount if available.
     * Falls back to heuristic if not a Kobold connection or if call fails.
     */
    suspend fun countTokensExact(text: String): Int {
        val client = koboldCPPClient
        if (client == null) {
            return estimateTokens(text) // Fallback
        }

        return client.countTokensExact(text)
    }
}
