package org.incept5.scheduler.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * JobbingTaskData is a data class that represents the data that is stored in the database for a scheduled task.
 * It will automatically serialise and deserialise the payload object to and from JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class JobbingTaskData(
    var correlationId: String? = null,
    var payloadClass: String? = null,
    var payload: String? = null,
    var repeatCount: Int = 0,
    @JsonIgnore
    @Transient
    private var payloadContent: Any? = null
) {
    companion object {
        private val MAPPER: ObjectMapper = ObjectMapperFactory.defaultMapper()
    }

    constructor(correlationId: String, payload: Any?) : this() {
        this.correlationId = correlationId
        if (payload != null) {
            this.payloadClass = payload.javaClass.name
            this.payload = serialize(payload)
        }
    }

    @JsonIgnore
    fun <T> getPayloadContent(): T? {
        if (payloadContent == null && payload != null) {
            try {
                payloadContent = MAPPER.readValue(payload, Class.forName(payloadClass))
            } catch (e: JsonProcessingException) {
                throw Exception("Failed to unmarshal json payload into class: $payloadClass", e)
            } catch (e: ClassNotFoundException) {
                throw Exception("Failed to unmarshal json payload into class: $payloadClass", e)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return payloadContent as T?
    }

    private fun serialize(payloadObject: Any): String {
        return try {
            MAPPER.writeValueAsString(payloadObject)
        } catch (e: JsonProcessingException) {
            throw Exception("Failed to serialize payload object: $payloadObject", e)
        }
    }
}