@file:Suppress("UNUSED_PARAMETER", "unused")

package io.justdevit.spring.logging.writer

import io.justdevit.spring.logging.RETURN_VALUE_NAME
import io.justdevit.spring.logging.Sensitive
import net.logstash.logback.marker.ObjectAppendingMarker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.event.Level
import java.util.function.Consumer

internal class LogstashJsonActionLogResolverTest {

    class TestClass {
        fun onStart() = Unit

        fun onStart(p0: String, @Sensitive p1: String) = Unit

        fun onFinishUnit() = Unit

        fun onFinishString(): String = "2"
    }

    private val onStartNonParametrizedMethod = TestClass::class.java.getMethod("onStart")
    private val onStartParametrizedMethod = TestClass::class.java.getMethod("onStart", String::class.java, String::class.java)
    private val onFinishUnitMethod = TestClass::class.java.getMethod("onFinishUnit")
    private val onFinishStringMethod = TestClass::class.java.getMethod("onFinishString")

    private val resolver = LogstashJsonActionLogResolver()

    @Test
    fun `Should return correct action log for on start non parametrized method`() {
        val context = OnStartLogContext(
            action = "TEST",
            method = onStartNonParametrizedMethod,
            level = Level.INFO,
            parameters = emptyList()
        )

        val result = resolver.onStart(context)

        with(result) {
            assertThat(message).isEqualTo("Action 'TEST' has started.")
            assertThat(arguments).isEmpty()
        }
    }

    @Test
    fun `Should return correct action log for on start parametrized method`() {
        val context = OnStartLogContext(
            action = "TEST",
            method = onStartParametrizedMethod,
            level = Level.INFO,
            parameters = listOf("V0", "V1")
        )

        val result = resolver.onStart(context)

        with(result) {
            assertThat(message).isEqualTo("Action 'TEST' has started.")
            assertThat(arguments).singleElement().satisfies(Consumer {
                assertThat(it).isInstanceOf(ObjectAppendingMarker::class.java)
                with(it as ObjectAppendingMarker) {
                    assertThat(fieldName).isEqualTo("p0")
                    assertThat(fieldValue).isEqualTo("V0")
                }
            })
        }
    }

    @Test
    fun `Should return correct action log on finish method (unit)`() {
        val context = OnFinishLogContext(
            action = "TEST",
            method = onFinishUnitMethod,
            level = Level.INFO,
            returnValue = null
        )

        val result = resolver.onFinish(context)

        with(result) {
            assertThat(message).isEqualTo("Action 'TEST' has successfully finished.")
            assertThat(arguments).isEmpty()
        }
    }

    @Test
    fun `Should return correct action log on finish method (string)`() {
        val context = OnFinishLogContext(
            action = "TEST",
            method = onFinishStringMethod,
            level = Level.INFO,
            returnValue = "2"
        )

        val result = resolver.onFinish(context)

        with(result) {
            assertThat(message).isEqualTo("Action 'TEST' has successfully finished.")
            assertThat(arguments).singleElement().satisfies(Consumer {
                assertThat(it).isInstanceOf(ObjectAppendingMarker::class.java)
                with(it as ObjectAppendingMarker) {
                    assertThat(fieldName).isEqualTo(RETURN_VALUE_NAME)
                    assertThat(fieldValue).isEqualTo(context.returnValue)
                }
            })
        }
    }

    @Test
    fun `Should return correct message on fail`() {
        val exception = IllegalArgumentException("MESSAGE")
        val context = OnThrowLogContext(
            action = "TEST",
            method = onStartNonParametrizedMethod,
            exception = exception
        )

        val result = resolver.onThrow(context)

        with(result) {
            assertThat(message).isEqualTo("Action 'TEST' has thrown an exception.")
            assertThat(arguments).containsExactly(exception)
        }
    }
}
