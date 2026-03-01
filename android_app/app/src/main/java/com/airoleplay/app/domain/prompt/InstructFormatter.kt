package com.airoleplay.app.domain.prompt

import com.airoleplay.app.data.remote.models.PromptMessage

enum class InstructFormat {
    CHATML, ALPACA, LLAMA3, MISTRAL, RAW
}

object InstructFormatter {

    fun format(messages: List<PromptMessage>, format: InstructFormat): String {
        return when (format) {
            InstructFormat.CHATML -> formatChatML(messages)
            InstructFormat.ALPACA -> formatAlpaca(messages)
            InstructFormat.LLAMA3 -> formatLlama3(messages)
            InstructFormat.MISTRAL -> formatMistral(messages)
            InstructFormat.RAW -> formatRaw(messages)
        }
    }

    private fun formatChatML(messages: List<PromptMessage>): String {
        val builder = java.lang.StringBuilder()
        for (message in messages) {
            builder.append("<|im_start|>${message.role}\n")
            builder.append("${message.content}<|im_end|>\n")
        }
        // KoboldCPP needs the assistant token prompt ready to write
        builder.append("<|im_start|>assistant\n")
        return builder.toString()
    }

    private fun formatAlpaca(messages: List<PromptMessage>): String {
        val builder = java.lang.StringBuilder()
        for (message in messages) {
            when (message.role) {
                "system" -> builder.append("${message.content}\n\n")
                "user" -> builder.append("### Instruction:\n${message.content}\n\n")
                "assistant" -> builder.append("### Response:\n${message.content}\n\n")
            }
        }
        builder.append("### Response:\n")
        return builder.toString()
    }

    private fun formatLlama3(messages: List<PromptMessage>): String {
        val builder = java.lang.StringBuilder()
        builder.append("<|begin_of_text|>")
        for (message in messages) {
            builder.append("<|start_header_id|>${message.role}<|end_header_id|>\n\n")
            builder.append("${message.content}<|eot_id|>")
        }
        builder.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return builder.toString()
    }

    private fun formatMistral(messages: List<PromptMessage>): String {
        val builder = java.lang.StringBuilder()
        var systemPrompt = ""

        for (message in messages) {
            if (message.role == "system") {
                systemPrompt = "${message.content}\n\n"
            } else if (message.role == "user") {
                builder.append("[INST] ${systemPrompt}${message.content} [/INST] ")
                systemPrompt = "" // Only prepend to the first user message
            } else if (message.role == "assistant") {
                builder.append("${message.content}\n")
            }
        }
        return builder.toString()
    }

    private fun formatRaw(messages: List<PromptMessage>): String {
        val builder = java.lang.StringBuilder()
        for (message in messages) {
            builder.append("${message.content}\n")
        }
        return builder.toString()
    }
}
