package com.example.incidentanalyst.aws

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import jakarta.inject.Inject
import java.time.Instant

@QuarkusTest
class MockCloudWatchAlarmClientTest {

    @Inject
    lateinit var mockCloudWatchAlarmClient: MockCloudWatchAlarmClient

    @Test
    fun `returns multiple severities`() {
        val result = mockCloudWatchAlarmClient.listAlarmsInAlarmState()

        org.hamcrest.MatcherAssert.assertThat(
            result,
            org.hamcrest.Matchers.instanceOf(AlarmQueryResult.Success::class.java)
        )

        val successResult = result as AlarmQueryResult.Success
        org.hamcrest.MatcherAssert.assertThat(
            successResult.alarms.size,
            org.hamcrest.Matchers.equalTo(3)
        )

        val alarmNames = successResult.alarms.map { it.alarmName }
        org.hamcrest.MatcherAssert.assertThat(
            alarmNames,
            org.hamcrest.Matchers.containsInAnyOrder(
                "HighCPUAlarm",
                "MediumDiskAlarm",
                "LowMemoryAlarm"
            )
        )
    }

    @Test
    fun `returns correct thresholds for each severity`() {
        val result = mockCloudWatchAlarmClient.listAlarmsInAlarmState()
        val successResult = result as AlarmQueryResult.Success

        val highSeverityAlarm = successResult.alarms.find { it.alarmName == "HighCPUAlarm" }
        org.hamcrest.MatcherAssert.assertThat(
            highSeverityAlarm?.threshold,
            org.hamcrest.Matchers.equalTo("90")
        )

        val mediumSeverityAlarm = successResult.alarms.find { it.alarmName == "MediumDiskAlarm" }
        org.hamcrest.MatcherAssert.assertThat(
            mediumSeverityAlarm?.threshold,
            org.hamcrest.Matchers.equalTo("70")
        )

        val lowSeverityAlarm = successResult.alarms.find { it.alarmName == "LowMemoryAlarm" }
        org.hamcrest.MatcherAssert.assertThat(
            lowSeverityAlarm?.threshold,
            org.hamcrest.Matchers.equalTo("50")
        )
    }

    @Test
    fun `returns alarms in ALARM state`() {
        val result = mockCloudWatchAlarmClient.listAlarmsInAlarmState()
        val successResult = result as AlarmQueryResult.Success

        successResult.alarms.forEach { alarm ->
            org.hamcrest.MatcherAssert.assertThat(
                alarm.stateValue,
                org.hamcrest.Matchers.equalTo("ALARM")
            )
        }
    }

    @Test
    fun `returns alarms with required fields`() {
        val result = mockCloudWatchAlarmClient.listAlarmsInAlarmState()
        val successResult = result as AlarmQueryResult.Success

        val alarm = successResult.alarms.first()

        org.hamcrest.MatcherAssert.assertThat(
            alarm.alarmName,
            org.hamcrest.Matchers.notNullValue()
        )
        org.hamcrest.MatcherAssert.assertThat(
            alarm.alarmDescription,
            org.hamcrest.Matchers.notNullValue()
        )
        org.hamcrest.MatcherAssert.assertThat(
            alarm.stateUpdatedTimestamp,
            org.hamcrest.Matchers.notNullValue()
        )
        org.hamcrest.MatcherAssert.assertThat(
            alarm.metricName,
            org.hamcrest.Matchers.notNullValue()
        )
        org.hamcrest.MatcherAssert.assertThat(
            alarm.namespace,
            org.hamcrest.Matchers.notNullValue()
        )
        org.hamcrest.MatcherAssert.assertThat(
            alarm.comparisonOperator,
            org.hamcrest.Matchers.notNullValue()
        )
    }
}
