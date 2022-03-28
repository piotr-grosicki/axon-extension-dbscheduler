package org.axonframework.extensions.dbscheduler.deadline

import org.axonframework.deadline.DeadlineMessage
import org.axonframework.deadline.GenericDeadlineMessage
import org.axonframework.messaging.Headers
import org.axonframework.messaging.MetaData
import org.axonframework.messaging.ScopeDescriptor
import org.axonframework.serialization.SerializedObject
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.SimpleSerializedObject
import java.time.Instant

class DeadlineTaskDataSerializer(
    private val serializer: Serializer
) {

    fun serialize(deadlineTaskData: DeadlineTask.Data): DeadlineTask.DataMap {
        val data = DeadlineTask.DataMap()
        val deadlineMessage = deadlineTaskData.deadlineMessage.get()
        val scopeDescriptor = deadlineTaskData.scopeDescriptor.get()

        data[Headers.DEADLINE_NAME] = deadlineTaskData.deadlineName
        data[Headers.MESSAGE_ID] = deadlineMessage.identifier
        data[Headers.MESSAGE_TIMESTAMP] = deadlineMessage.timestamp.toString()

        val serializedPayload = serializeToBytes(deadlineMessage.payload)
        data[Headers.SERIALIZED_MESSAGE_PAYLOAD] = serializedPayload.data
        data[Headers.MESSAGE_TYPE] = serializedPayload.type.name
        data[Headers.MESSAGE_REVISION] = serializedPayload.type.revision
        data[Headers.MESSAGE_METADATA] = serializeToBytes(deadlineMessage.metaData).data

        val serializedScopeDescriptor = serializeToBytes(scopeDescriptor)
        data[SERIALIZED_DEADLINE_SCOPE] = serializedScopeDescriptor.data
        data[SERIALIZED_DEADLINE_SCOPE_CLASS_NAME] = serializedScopeDescriptor.type.name

        return data
    }

    fun deserialize(rawData: Map<String, Any?>): DeadlineTask.Data =
        DeadlineTask.Data(
            deadlineName = rawData[Headers.DEADLINE_NAME].toString(),
            deadlineMessage = { deserializeDeadlineMessage(rawData) },
            scopeDescriptor = { deserializeScopeDescriptor(rawData) }
        )

    private fun deserializeScopeDescriptor(rawData: Map<String, Any?>): ScopeDescriptor =
        serializer.deserialize(
            SimpleSerializedObject(
                rawData[SERIALIZED_DEADLINE_SCOPE] as ByteArray,
                ByteArray::class.java,
                rawData[SERIALIZED_DEADLINE_SCOPE_CLASS_NAME].toString(),
                null
            )
        )

    private fun deserializeDeadlineMessage(rawData: Map<String, Any?>): DeadlineMessage<Any> =
        GenericDeadlineMessage(
            rawData[Headers.DEADLINE_NAME].toString(),
            rawData[Headers.MESSAGE_ID].toString(),
            deserializeDeadlineMessagePayload(rawData),
            deserializeDeadlineMessageMetaData(rawData),
            Instant.parse(rawData[Headers.MESSAGE_TIMESTAMP].toString())
        )

    private fun deserializeDeadlineMessageMetaData(rawData: Map<String, Any?>): Map<String, Any> =
        serializer.deserialize(
            SimpleSerializedObject(
                rawData[Headers.MESSAGE_METADATA] as ByteArray,
                ByteArray::class.java,
                MetaData::class.java.name,
                null
            )
        )

    private fun deserializeDeadlineMessagePayload(rawData: Map<String, Any?>): Any =
        serializer.deserialize(
            SimpleSerializedObject(
                rawData[Headers.SERIALIZED_MESSAGE_PAYLOAD] as ByteArray,
                ByteArray::class.java,
                rawData[Headers.MESSAGE_TYPE].toString(),
                rawData[Headers.MESSAGE_REVISION].toString()
            )
        )

    private fun serializeToBytes(payload: Any?): SerializedObject<ByteArray> =
        serializer.serialize(payload, ByteArray::class.java)

    companion object {
        const val SERIALIZED_DEADLINE_SCOPE = "serializedDeadlineScope"
        const val SERIALIZED_DEADLINE_SCOPE_CLASS_NAME = "serializedDeadlineScopeClassName"
    }
}