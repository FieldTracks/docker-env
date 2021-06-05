package org.fieldtracks.middleware

import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.fixedRateTimer

interface MessageReciver {
    fun onMessageReceived(topic: String, message: String)
    fun mqttTopics(): List<String>
}

class ScheduledBatchReceiver<RESULT,UPDATE>(
    private val name: String, // To be used in scheduling Thread
    private val processor: BatchProcessor<RESULT,UPDATE>,
    private val mqttClient: MqttAsyncClient,
    private val updateTopicStructure: Pair<String, Class<UPDATE>>,
    private val resultTopicStructure: Pair<String, Class<RESULT>>,
    private val updateTopicPattern: String = updateTopicStructure.first,
   private val maxBatchSize:Int = 100_000, // Upper limit for batch-size
): MessageReciver {
    private val updateQueue = ConcurrentLinkedQueue<UPDATE>()
    private val prevResultQueue = ConcurrentLinkedQueue<RESULT>()
    private val log = LoggerFactory.getLogger(ScheduledBatchReceiver::class.java)
    private val gson = Gson()

    // To be called, when a new MQTT-Message is received
    override fun onMessageReceived(topic: String, message: String) {
        try {
            if(topic.startsWith(updateTopicStructure.first)){
                updateQueue.add(gson.fromJson(message,updateTopicStructure.second))
            } else if(topic.startsWith(resultTopicStructure.first)) {
                prevResultQueue.add(gson.fromJson(message,resultTopicStructure.second))
            }
        } catch (e: Exception) {
            log.error("Skipping non-parseable message in topic $topic - content: $message", e)
            return
        }
    }

    fun schedule( delay: Long, interval: Long): ScheduledBatchReceiver<RESULT,UPDATE> {
        fixedRateTimer(name,false,delay,interval) {
            processBatch()
        }
        return this
    }

    override fun  mqttTopics(): List<String> {
        return listOf(updateTopicPattern,resultTopicStructure.first)
    }


    internal fun processBatch() {
        val elements = ArrayList<UPDATE>()
        val receivedResults = ArrayList<RESULT>()
        var currentElement = updateQueue.poll()
        var cnt = 0
        while(currentElement != null && cnt < maxBatchSize) {
            cnt++
            elements += currentElement
            currentElement = updateQueue.poll()
        }
        if(cnt == maxBatchSize) {
            log.error("Received more than $maxBatchSize messages for $name. Possible message strom")
        }
        var receivedResult = prevResultQueue.poll()
        var resultCnt = 0
        while (receivedResult != null) {
            resultCnt++
            receivedResults += receivedResult
            receivedResult = prevResultQueue.poll()
        }
        if(resultCnt > 1) {
            log.error("Multiple aggregators for $name. - Received $resultCnt previous results while collecting reports")
        }
        publish(processor.processBatch(receivedResults.lastOrNull(),elements))
    }

    private fun publish(result: RESULT?) {
        if(result != null) {
            val payload = gson.toJson(result).toByteArray(Charset.forName("UTF-8"))
            mqttClient.publish(resultTopicStructure.first,payload,0,true)
        }
    }
}
interface BatchProcessor<RESULT,UPDATE>{
    fun processBatch(prevResult: RESULT?, messages: List<UPDATE>): RESULT?
}
