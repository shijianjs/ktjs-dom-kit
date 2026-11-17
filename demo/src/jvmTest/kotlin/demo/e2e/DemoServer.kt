package demo.e2e

import cn.hutool.http.server.SimpleServer
import java.io.File
import java.net.ServerSocket
import java.nio.file.Path

/**
 * Serves the production bundle from the test JVM, standing in for the static web server the demo
 * page needs.
 */
object DemoServer {
    private val root: File = File(
        System.getProperty("demo.dist")
            ?: Path.of("build", "dist", "js", "productionExecutable").toString(),
    ).canonicalFile

    /**
     * An ephemeral port from the OS. `SimpleServer(0)` does **not** mean that: Hutool picks a random
     * port anywhere in 1024–65535, and Chromium refuses a few of those outright
     * (`ERR_UNSAFE_PORT`, e.g. 6665–6669), which made runs fail unrelated to the code under test.
     */
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private val server: SimpleServer by lazy {
        val started = SimpleServer(freePort()).setRoot(root)
        started.start()
        Runtime.getRuntime().addShutdownHook(Thread { started.rawServer.stop(0) })
        started
    }

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}/"
}
