package com.schooldevops.democompletefuture

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.stream.Collectors

/**
 * from: https://dzone.com/articles/20-examples-of-using-javas-completablefuture
 */
class CFTest01 {

    @Test
    fun completableFutureExample() {
        val cf = CompletableFuture.completedFuture("message")
        assertTrue(cf.isDone())
        assertEquals("message", cf.getNow(null))
    }

    @Test
    fun runAsyncExample() {

        /**
         * CompletableFuture 메서드가 일반적으로 Async키워드로 끝나는 경우 비동기적으로 실행된다.
         * 기본적으로 Executor가 지정되지 않은경우 비동기 실행은 데몬 스레드를 사용하여 Runnable 작업을 실행하는 공통 ForkJoinPool을 구현한다.
         * 이는 CompletableFuture에만 해당된다. 다른 CompletionStage구현은 기본 동작을 재정의 할 수 있다.
         */
        val cf = CompletableFuture.runAsync{
            assertTrue(Thread.currentThread().isDaemon)
            Thread.sleep(100)
        }

        assertFalse(cf.isDone)
        Thread.sleep(1000)
        assertTrue(cf.isDone)
    }

    @Test
    fun thenApplyExample() {

        /**
         * then: 의 의미는 현재 단계가 정상적으로 완료되면(예외없이) 이 단계의 작업이 발생함을 의미한다.
         *      이 경우 현재 단계는 이미 "message"값으로 완료되었다.
         * apply: 이 의미는 반환된 단계가 이전 단계의 결과에 함수를 적용한다는 의미이다.
         * Function의 실행은 블록이 되고, uppercase 작업이 완료된 경우에만 getNow()에 도달한다.
         */
        val cf = CompletableFuture.completedFuture("message")
            .thenApply {
                assertFalse(/* condition = */ Thread.currentThread().isDaemon)
                it.toString().uppercase()
            }

        assertEquals("MESSAGE", cf.getNow(null))
    }

    @Test
    fun thenApplyAsyncExample() {

        /**
         * 비동기 접미사를 추가하여 연결된 CompletableFuture가 비동기적으로 실행되도록 한다.
         * ForkJoinPoo.commonPool()을 사용한다.
         */
        val cf = CompletableFuture.completedFuture("message").thenApplyAsync {
            assertTrue(Thread.currentThread().isDaemon)
            Thread.sleep(100)
            it.toString().uppercase()
        }

        assertNull(cf.getNow(null))
        assertEquals("MESSAGE", cf.join());
    }

    @Test
    fun thenApplyAsyncWithExecutorExample() {

        /**
         * 비동기 메서드의 매우 유용한 기능은 원하는 CompletableFuture를 실행하는데 사용할 수 있는 Executor를 제공하는 기능이다.
         * 이 에에서는 고정 스레드풀을 사용하여 대문자 변환 기능을 적용하는 방법을 보여준다.
         */
        val cf = CompletableFuture.completedFuture("message").thenApplyAsync( {
            println(Thread.currentThread().name)
            assertTrue(Thread.currentThread().name.startsWith("custom-executor-"))
            assertFalse(Thread.currentThread().isDaemon)
            Thread.sleep(100)
            it.toString().uppercase()
        }, Executors.newFixedThreadPool(3, ThreadFactoryImpl()))

        assertNull(cf.getNow(null))
        assertEquals("MESSAGE", cf.join())
    }

    @Test
    fun theAcceptExample() {
        /**
         * 다음 단계가 현재 단계의 결과를 허용하지만 계산에서 값을 변환할 필요가 없는경우
         * 즉, 반환 유형이 void 인경우, 함수를 적용하는 대신 소비자를 허용할수 있으므로 thenAccept 메소드가 사용되었다.
         *
         * Consumer는 동기적으로 실행되므로 반환된 CompatableFuture에 참여할 필요가 없다.
         */
        val result = StringBuilder()
        CompletableFuture.completedFuture("thenAccept message")
            .thenAccept{ result.append(it) }
        assertTrue(result.length > 0, "Result was emtpy")
    }

    @Test
    fun thenAcceptAsyncExample() {
        /**
         * 이번에도 thenAccept의 비동기 버젼을 사용하면 연결된 CompletableFuture가 비동기적으로 실행된다.
         */
        val result = StringBuilder()
        val cf = CompletableFuture.completedFuture("thenAcceptAsync message")
            .thenAcceptAsync { result.append(it) }
        cf.join()
        assertTrue(result.length > 0, "Result was empty")

    }

//    @Test
//    fun thenComposeExample() {
//        val original = "Message"
//        CompletableFuture.completedFuture(original).thenApply { it.toString().uppercase() }
//            .thenCompose { upper -> CompletableFuture.completedFuture(original).thenApply { it.toString().lowercase() } }
//            .thenApply { s -> upper + s }
//    }

    @Test
    fun anyOfExample() {
        val result = StringBuilder()
        val messages = listOf("a", "b", "c")

        val futures = messages.stream()
            .map {
                CompletableFuture.completedFuture(it).thenApply {
                    it.toString().uppercase()
                }
            }
            .collect(Collectors.toList())


        val toTypedArray = futures.toTypedArray()
        CompletableFuture.anyOf(*futures.toTypedArray()).whenComplete { res, th ->
            if (th == null) {
                assertTrue(isUpperCase(res.toString()))
                result.append(res)
            }
        }

        assertTrue(result.isNotEmpty(), "Result was emtpy")
    }


    @Test
    fun allOfExample() {
        val result = StringBuilder()
        val messages = listOf("a", "b", "c")

        val futures = messages.stream()
            .map {
                CompletableFuture.completedFuture(it).thenApply {
                    it.toString().uppercase()
                }
            }
            .collect(Collectors.toList())


        val toTypedArray = futures.toTypedArray()
        val allOf = CompletableFuture.allOf(*futures.toTypedArray()).whenComplete { res, th ->
            futures.forEach { assertTrue(isUpperCase(it.getNow(null))) }
            result.append("done")
        }

        allOf.join()

        assertTrue(result.isNotEmpty(), "Result was emtpy")
    }

    @Test
    fun allOfAsyncExample() {
        val result = StringBuilder()
        val messages = listOf("a", "b", "c")

        val futures =
            messages.stream().map { CompletableFuture.completedFuture(it).thenApplyAsync { it.toString().uppercase() } }
                .toList()

        val allOf = CompletableFuture.allOf(*futures.toTypedArray()).whenComplete { res, th ->
            futures.forEach { assertTrue(isUpperCase(it.getNow(null))) }
            result.append("done")
        }

        allOf.join()

        assertTrue(result.isNotEmpty(), "Result was emtpy")

    }

    private fun isUpperCase(toString: String): Boolean {
        val regex = Regex("[A-Z]+")
        return regex.matches(toString)
    }


}

class ThreadFactoryImpl() : ThreadFactory {
    var count: Int = 1
    override fun newThread(r: Runnable): Thread {
        return Thread(r, "custom-executor-" + count++)
    }

}