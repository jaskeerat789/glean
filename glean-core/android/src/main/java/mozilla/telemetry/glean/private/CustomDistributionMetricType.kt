/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.telemetry.glean.private

import com.sun.jna.StringArray
import androidx.annotation.VisibleForTesting
import mozilla.telemetry.glean.Glean
import mozilla.telemetry.glean.rust.getAndConsumeRustString
import mozilla.telemetry.glean.rust.LibGleanFFI
import mozilla.telemetry.glean.rust.toByte
import mozilla.telemetry.glean.Dispatchers
import mozilla.telemetry.glean.rust.toBoolean

/**
 * This implements the developer facing API for recording custom distribution metrics.
 *
 * Custom distributions are histograms with the following parameters that are settable on a
 * per-metric basis:
 *
 *    - `rangeMin`/`rangeMax`: The minimum and maximum values
 *    - `bucketCount`: The number of histogram buckets
 *    - `histogramType`: Whether the bucketing is linear or exponential
 *
 * This metric exists primarily for backward compatibility with histograms in
 * legacy (pre-Glean) telemetry, and its use is not recommended for newly-created
 * metrics.
 *
 * Instances of this class type are automatically generated by the parsers at build time,
 * allowing developers to record values that were previously registered in the metrics.yaml file.
 */
data class CustomDistributionMetricType(
    private var handle: Long,
    private val disabled: Boolean,
    private val sendInPings: List<String>
) : HistogramBase {
    /**
     * The public constructor used by automatically generated metrics.
     */
    constructor(
        disabled: Boolean,
        category: String,
        lifetime: Lifetime,
        name: String,
        sendInPings: List<String>,
        rangeMin: Long,
        rangeMax: Long,
        bucketCount: Int,
        histogramType: HistogramType
    ) : this(handle = 0, disabled = disabled, sendInPings = sendInPings) {
        val ffiPingsList = StringArray(sendInPings.toTypedArray(), "utf-8")
        this.handle = LibGleanFFI.INSTANCE.glean_new_custom_distribution_metric(
            category = category,
            name = name,
            send_in_pings = ffiPingsList,
            send_in_pings_len = sendInPings.size,
            lifetime = lifetime.ordinal,
            disabled = disabled.toByte(),
            range_min = rangeMin,
            range_max = rangeMax,
            bucket_count = bucketCount.toLong(),
            histogram_type = histogramType.ordinal
        )
    }

    /**
     * Destroy this metric.
     */
    protected fun finalize() {
        if (this.handle != 0L) {
            LibGleanFFI.INSTANCE.glean_destroy_custom_distribution_metric(this.handle)
        }
    }

    override fun accumulateSamples(samples: LongArray) {
        if (disabled) {
            return
        }

        // The reason we're using [Long](s) instead of [UInt](s) in Kotlin-land is
        // the lack of [UInt] (in stable form). The positive part of [Int] would not
        // be enough to represent the values coming in:
        // - [UInt.MAX_VALUE] is 4294967295
        // - [Int.MAX_VALUE] is 2147483647
        // - [Long.MAX_VALUE] is 9223372036854775807
        //
        // On the rust side, Long(s) are handled as i64 and then casted to u64.
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.launch {
            LibGleanFFI.INSTANCE.glean_custom_distribution_accumulate_samples(
                Glean.handle,
                this@CustomDistributionMetricType.handle,
                samples,
                samples.size
            )
        }
    }

    /**
     * Tests whether a value is stored for the metric for testing purposes only.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.
     *                 Defaults to the first value in `sendInPings`.
     * @return true if metric value exists, otherwise false
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmOverloads
    fun testHasValue(pingName: String = sendInPings.first()): Boolean {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        return LibGleanFFI
            .INSTANCE.glean_custom_distribution_test_has_value(Glean.handle, this.handle, pingName)
            .toBoolean()
    }

    /**
     * Returns the stored value for testing purposes only.
     *
     * @param pingName represents the name of the ping to retrieve the metric for.
     *                 Defaults to the first value in `sendInPings`.
     * @return value of the stored metric
     * @throws [NullPointerException] if no value is stored
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmOverloads
    fun testGetValue(pingName: String = sendInPings.first()): DistributionData {
        @Suppress("EXPERIMENTAL_API_USAGE")
        Dispatchers.API.assertInTestingMode()

        if (!testHasValue(pingName)) {
            throw NullPointerException()
        }

        val ptr = LibGleanFFI.INSTANCE.glean_custom_distribution_test_get_value_as_json_string(
                Glean.handle,
                this.handle,
                pingName)!!

        return DistributionData.fromJsonString(ptr.getAndConsumeRustString())!!
    }
}
