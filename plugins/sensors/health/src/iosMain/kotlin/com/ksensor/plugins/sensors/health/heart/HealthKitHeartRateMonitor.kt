package com.ksensor.plugins.sensors.health.heart

import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.HealthKit.*
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

class HealthKitHeartRateMonitor : IosHeartRateMonitor {
    private val healthStore = if (HKHealthStore.isHealthDataAvailable()) HKHealthStore() else null
    private val heartRateType = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate)!!

    override fun isSupported(): Boolean {
        return HKHealthStore.isHealthDataAvailable()
    }

    override fun start(): Flow<KSensorResponse<SensorData.HeartRate>> = callbackFlow {
        if (healthStore == null) {
            close()
            return@callbackFlow
        }

        val query = HKAnchoredObjectQuery(
            type = heartRateType,
            predicate = null,
            anchor = null,
            limit = HKObjectQueryNoLimit,
            resultsHandler = { _, samples, _, _, _ ->
                processSamples(samples as? List<HKQuantitySample>, this)
            }
        )

        query.updateHandler = { _, samples, _, _, _ ->
            processSamples(samples as? List<HKQuantitySample>, this)
        }

        healthStore.executeQuery(query)

        awaitClose {
            healthStore.stopQuery(query)
        }
    }

    private fun processSamples(samples: List<HKQuantitySample>?, scope: ProducerScope<KSensorResponse<SensorData.HeartRate>>) {
        samples?.lastOrNull()?.let { sample ->
            val unit = HKUnit.unitFromString("count/min")
            val bpm = sample.quantity.doubleValueForUnit(unit).toFloat()
            val timestamp = (sample.startDate.timeIntervalSince1970 * 1000).toLong()
            
            scope.trySend(KSensorResponse(SensorData.HeartRate(
                heartRate = bpm,
                source = SensorData.HeartRateSource.HARDWARE_SENSOR,
                timestamp = timestamp
            )))
        }
    }
}
