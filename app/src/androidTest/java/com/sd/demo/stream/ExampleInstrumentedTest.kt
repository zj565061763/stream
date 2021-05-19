package com.sd.demo.stream

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sd.demo.stream.utils.TestDefaultStream
import com.sd.demo.stream.utils.TestStream
import com.sd.lib.stream.DefaultStreamManager
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
        DefaultStreamManager.register(TestDefaultStream::class.java)

        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "0"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return "hello tag"
            }
        }

        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream3 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "3"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        FStreamManager.getInstance().run {
            this.register(stream0)
            this.register(stream1).setPriority(-1)
            this.register(stream2)
            this.register(stream3).setPriority(1)
        }

        Assert.assertEquals(0, FStreamManager.getInstance().getConnection(stream0)!!.getPriority(TestStream::class.java))
        Assert.assertEquals(-1, FStreamManager.getInstance().getConnection(stream1)!!.getPriority(TestStream::class.java))
        Assert.assertEquals(0, FStreamManager.getInstance().getConnection(stream2)!!.getPriority(TestStream::class.java))
        Assert.assertEquals(1, FStreamManager.getInstance().getConnection(stream3)!!.getPriority(TestStream::class.java))

        val listResult = mutableListOf<Any?>()
        val proxy = FStream.ProxyBuilder()
            .setResultFilter(object : FStream.ResultFilter {
                override fun filter(method: Method, methodParams: Array<Any?>?, results: List<Any?>): Any? {
                    listResult.addAll(results)
                    return results.last()
                }
            })
            .build(TestStream::class.java)

        listResult.clear()
        Assert.assertEquals("1", proxy.getContent("http"))
        Assert.assertEquals(3, listResult.size)
        Assert.assertEquals("3", listResult[0])
        Assert.assertEquals("2", listResult[1])
        Assert.assertEquals("1", listResult[2])

        val stream4 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "4"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        FStreamManager.getInstance().register(stream4)

        listResult.clear()
        Assert.assertEquals("1", proxy.getContent("http"))
        Assert.assertEquals(4, listResult.size)
        Assert.assertEquals("3", listResult[0])
        Assert.assertEquals("2", listResult[1])
        Assert.assertEquals("4", listResult[2])
        Assert.assertEquals("1", listResult[3])

        DefaultStreamManager.unregister(TestDefaultStream::class.java)
        FStreamManager.getInstance().run {
            this.unregister(stream0)
            this.unregister(stream1)
            this.unregister(stream2)
            this.unregister(stream3)
            this.unregister(stream4)
        }

        Assert.assertEquals(null, FStreamManager.getInstance().getConnection(stream0))
        Assert.assertEquals(null, FStreamManager.getInstance().getConnection(stream1))
        Assert.assertEquals(null, FStreamManager.getInstance().getConnection(stream2))
        Assert.assertEquals(null, FStreamManager.getInstance().getConnection(stream3))
        Assert.assertEquals(null, FStreamManager.getInstance().getConnection(stream4))
    }

    @Test
    fun testDefaultStream() {
        DefaultStreamManager.register(TestDefaultStream::class.java)

        val proxy = FStream.ProxyBuilder().build(TestStream::class.java)
        val result = proxy.getContent("http")
        Assert.assertEquals("default@http", result)

        DefaultStreamManager.unregister(TestDefaultStream::class.java)

        Assert.assertEquals(null, proxy.getContent("http"))
    }

    @Test
    fun testDispatchCallbackBefore() {
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream3 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "3"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        FStreamManager.getInstance().run {
            this.register(stream1)
            this.register(stream2)
            this.register(stream3)
        }

        val proxy = FStream.ProxyBuilder()
            .setDispatchCallback(object : FStream.DispatchCallback {
                override fun beforeDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?): Boolean {
                    return true
                }

                override fun afterDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?, methodResult: Any?): Boolean {
                    return false
                }
            })
            .build(TestStream::class.java)

        val result = proxy.getContent("http")
        Assert.assertEquals(null, result)

        FStreamManager.getInstance().run {
            this.unregister(stream1)
            this.unregister(stream2)
            this.unregister(stream3)
        }
    }

    @Test
    fun testDispatchCallbackAfter() {
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream3 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "3"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        FStreamManager.getInstance().run {
            this.register(stream1)
            this.register(stream2)
            this.register(stream3)
        }

        val listResult = mutableListOf<Any?>()
        val proxy = FStream.ProxyBuilder()
            .setResultFilter(object : FStream.ResultFilter {
                override fun filter(method: Method, methodParams: Array<Any?>?, results: List<Any?>): Any? {
                    listResult.addAll(results)
                    return results.last()
                }
            })
            .setDispatchCallback(object : FStream.DispatchCallback {
                override fun beforeDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?): Boolean {
                    return false
                }

                override fun afterDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?, methodResult: Any?): Boolean {
                    return "2" == methodResult
                }
            })
            .build(TestStream::class.java)

        val result = proxy.getContent("http")
        Assert.assertEquals("2", result)
        Assert.assertEquals(2, listResult.size)
        Assert.assertEquals("1", listResult[0])
        Assert.assertEquals("2", listResult[1])

        FStreamManager.getInstance().run {
            this.unregister(stream1)
            this.unregister(stream2)
            this.unregister(stream3)
        }
    }

    @Test
    fun testDispatchBreak() {
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                FStreamManager.getInstance().getConnection(this)!!.breakDispatch(TestStream::class.java)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream3 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "3"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        FStreamManager.getInstance().run {
            this.register(stream1)
            this.register(stream2)
            this.register(stream3)
        }

        val listResult = mutableListOf<Any?>()
        val proxy = FStream.ProxyBuilder()
            .setResultFilter(object : FStream.ResultFilter {
                override fun filter(method: Method, methodParams: Array<Any?>?, results: List<Any?>): Any? {
                    listResult.addAll(results)
                    return results.last()
                }
            })
            .build(TestStream::class.java)

        val result = proxy.getContent("http")
        Assert.assertEquals("2", result)
        Assert.assertEquals(2, listResult.size)
        Assert.assertEquals("1", listResult[0])
        Assert.assertEquals("2", listResult[1])

        FStreamManager.getInstance().run {
            this.unregister(stream1)
            this.unregister(stream2)
            this.unregister(stream3)
        }
    }
}