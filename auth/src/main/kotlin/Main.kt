import com.sun.net.httpserver.HttpServer
import controller.AuthController
import repository.UserRepository
import service.UserService
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val userRepository = UserRepository()
    val userService = UserService(userRepository)
    val authController = AuthController(userService)
    val server = HttpServer.create(InetSocketAddress(5454), 0)
    println("Server running at http://localhost:5454")

    server.createContext("/register") { exchange ->
        if (exchange.requestMethod == "POST") authController.handleRegister(exchange)
        else exchange.sendResponseHeaders(405, 0).also { exchange.responseBody.close() }
    }
    server.executor = null
    server.start()

}