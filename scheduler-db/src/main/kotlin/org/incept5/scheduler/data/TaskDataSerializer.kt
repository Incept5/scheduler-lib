package org.incept5.scheduler.data

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.github.kagkarlsson.scheduler.serializer.Serializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * This is an instance of Serializer that assumes that the task data is a JobbingTaskData
 * which supports serialisation to and from JSON.
 */
class TaskDataSerializer : Serializer {
    private val objectReader: ObjectReader = ObjectMapperFactory.readerFor(JobbingTaskData::class.java)
    private val objectWriter: ObjectWriter = ObjectMapperFactory.writerFor(JobbingTaskData::class.java)

    override fun serialize(data: Any?): ByteArray? {
        data ?: return null

        return try {
            ByteArrayOutputStream().use { output ->
                objectWriter.writeValue(output, data)
                output.toByteArray()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(aClass: Class<T>, data: ByteArray?): T? {
        data ?: return null

        return try {
            ByteArrayInputStream(data).use { inputStream ->
                objectReader.readValue(inputStream, JobbingTaskData::class.java) as T
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}