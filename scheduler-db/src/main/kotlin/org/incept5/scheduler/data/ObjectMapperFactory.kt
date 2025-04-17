package org.incept5.scheduler.data

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Statically accessible Jackson ObjectMapper factory is needed for the
 * TaskDataSerializer to work correctly.
 */
class ObjectMapperFactory {
    companion object {
        private val objectMapper: ObjectMapper = ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())

        fun defaultMapper(): ObjectMapper = objectMapper

        fun readerFor(clazz: Class<*>): ObjectReader {
            val type = TypeFactory.defaultInstance().constructType(clazz)
            return objectMapper.readerFor(type)
        }

        fun writerFor(clazz: Class<*>): ObjectWriter {
            val type = TypeFactory.defaultInstance().constructType(clazz)
            return objectMapper.writerFor(type)
        }
    }
}