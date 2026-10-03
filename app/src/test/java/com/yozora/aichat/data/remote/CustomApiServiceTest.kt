package com.yozora.aichat.data.remote

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import com.yozora.aichat.data.db.MessageEntity
import com.yozora.aichat.data.db.PersonaEntity
import com.yozora.aichat.ui.chat.ApiVendor
import com.yozora.aichat.ui.chat.SafetyLevel
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class CustomApiServiceTest {
    @Test
    fun customCompletionUsesConfiguredEndpointKeyModelAndConversation() = withServer(
        """{"choices":[{"message":{"content":"Custom provider reply"}}],"usage":{"prompt_tokens":8,"completion_tokens":4,"total_tokens":12}}"""
    ) { config, requests ->
        val reply = GeminiChatService().sendMessage(
            apiKey = "test-custom-key", vendor = ApiVendor.Custom,
            persona = persona(config), safetyLevel = SafetyLevel.None, images = emptyList(),
            history = listOf(MessageEntity(chatId = "chat", role = "model", content = "Earlier reply", timestamp = 0)),
            userInput = "Next line", webSearchEnabled = false
        ).getOrThrow()
        assertEquals("Custom provider reply", reply.text)
        assertEquals(12, reply.totalTokenCount)
        val request = requests.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/gateway/v1/chat/completions", request.path)
        assertEquals("POST", request.method)
        assertEquals("Bearer test-custom-key", request.getHeader("Authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("custom-model", body.getString("model"))
        assertEquals("assistant", body.getJSONArray("messages").getJSONObject(1).getString("role"))
        assertEquals("Next line", body.getJSONArray("messages").getJSONObject(2).getString("content"))
    }

    @Test
    fun fetchModelsUsesSameEndpointAndKeyWithoutRequiringModel() = withServer(
        """{"data":[{"id":"model-b"},{"id":"model-a"},{"id":"model-b"}]}"""
    ) { config, requests ->
        assertEquals(listOf("model-a", "model-b"), GeminiChatService().fetchCustomModels(config, "model-list-key").getOrThrow())
        val request = requests.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/gateway/v1/models", request.path)
        assertEquals("GET", request.method)
        assertEquals("Bearer model-list-key", request.getHeader("Authorization"))
    }

    @Test
    fun customToolPlanningUsesCustomConnection() = withServer(
        """{"choices":[{"message":{"content":"","tool_calls":[{"function":{"name":"web_search","arguments":"{\"query\":\"sky\"}"}}]}}]}"""
    ) { config, requests ->
        val plan = GeminiChatService().planToolUse(
            apiKey = "tools-key", vendor = ApiVendor.Custom, persona = persona(config),
            history = emptyList(), userInput = "Search the sky", safetyLevel = SafetyLevel.None,
            images = emptyList(), enabledTools = setOf("web_search")
        ).getOrThrow()
        assertEquals("web_search", plan.functionCall!!.name)
        val request = requests.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/gateway/v1/chat/completions", request.path)
        assertTrue(JSONObject(request.body.readUtf8()).has("tools"))
    }

    @Test
    fun providerErrorsArePreservedAndNotRetriedOnAnotherHost() = withServer(
        """{"error":{"message":"Project access denied"}}""", status = 403
    ) { config, requests ->
        val result = GeminiChatService().fetchCustomModels(config, "denied-key")
        val failure = result.exceptionOrNull() as ApiHttpException
        assertEquals(403, failure.statusCode)
        assertEquals("Project access denied", failure.apiMessage)
        assertEquals(1, requests.requestCount)
    }

    @Test
    fun customModelLookupUsesConfiguredHttpProxy() = withServer(
        """{"data":[{"id":"proxy-model"}]}"""
    ) { config, requests ->
        val proxyConfig = CustomApiConfig(
            baseUrl = "http://unreachable.invalid/v1",
            proxyUrl = config.baseUrl.substringBefore("/gateway")
        )
        assertEquals(listOf("proxy-model"), GeminiChatService().fetchCustomModels(proxyConfig, "proxy-key").getOrThrow())
        val request = requests.takeRequest(2, TimeUnit.SECONDS)!!
        assertTrue(request.requestLine.startsWith("GET http://unreachable.invalid/v1/models "))
        assertEquals("Bearer proxy-key", request.getHeader("Authorization"))
    }

    @Test
    fun customModelLookupDoesNotForwardKeyThroughRedirects() = withServer(
        "{}", status = 302, redirect = "/redirected"
    ) { config, requests ->
        val error = GeminiChatService().fetchCustomModels(config, "redirect-key").exceptionOrNull() as ApiHttpException
        assertEquals(302, error.statusCode)
        assertEquals(1, requests.requestCount)
    }

    private fun persona(config: CustomApiConfig) = PersonaEntity(
        name = "Test", avatarUri = null, systemPrompt = "Stay in character", model = "custom-model", customApi = config
    )

    private fun withServer(
        response: String,
        status: Int = 200,
        redirect: String? = null,
        block: suspend (CustomApiConfig, MockWebServer) -> Unit
    ) = runBlocking {
        MockWebServer().use { server ->
            val reply = MockResponse().setResponseCode(status)
                .setHeader("Content-Type", "application/json").setBody(response)
            redirect?.let { reply.setHeader("Location", it) }
            server.enqueue(reply)
            server.start()
            block(CustomApiConfig(server.url("/gateway/v1").toString()), server)
        }
    }
}
