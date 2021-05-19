package com.sd.demo.stream

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sd.demo.stream.utils.TestStream
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Method

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testNormal() {
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                FStreamManager.getInstance().unregister(this)
                Assert.assertEquals(null, FStreamManager.getInstance().getConnection(this))
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                FStreamManager.getInstance().unregister(this)
                Assert.assertEquals(null, FStreamManager.getInstance().getConnection(this))
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream3 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                FStreamManager.getInstance().unregister(this)
                Assert.assertEquals(null, FStreamManager.getInstance().getConnection(this))
                return "3"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        FStreamManager.getInstance().run {
            this.register(stream1)
            this.register(stream2)
            this.register(stream3).setPriority(1)
        }

        val listResult = mutableListOf<Any?>()
        val proxy = FStream.ProxyBuilder()
            .setResultFilter(object : FStream.ResultFilter {
                override fun filter(method: Method, methodParams: Array<Any?>?, results: List<Any?>): Any? {
                    listResult.addAll(results)
                    return results.first()
                }
            })
            .build(TestStream::class.java)

        val result = proxy.getContent("http")
        Assert.assertEquals("3", result)
        Assert.assertEquals(3, listResult.size)
        Assert.assertEquals("3", listResult[0])
        Assert.assertEquals("1", listResult[1])
        Assert.assertEquals("2", listResult[2])
    }
}