package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.dao.DAOFacade
import com.example.dao.DAOFacadeCache
import com.example.dao.DAOFacadeDatabase
import com.example.negocio.routeRegistrosDeCompra
import com.fasterxml.jackson.databind.SerializationFeature
import com.mchange.v2.c3p0.ComboPooledDataSource
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.response.*
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.io.IOException
import java.sql.Driver
import java.text.DateFormat


// TODO introduzir teste unitario
// TODO modular as classes de dados Contas por exemplo
// TODO introduzir autenticacao do google


@KtorExperimentalLocationsAPI
@Location("/registrosDeCompra") class RouteRegistrosDeCompra


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


/**
 * Trecho relacionado a [Authentication]
 */
open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}


/**
 * Trecho relacionado a [Exception]s
 */
//class InvalidCredentialsException(message: String) : RuntimeException(message)
//class SistemaException(message: String) : RuntimeException(message)
//class NegocioException(message: String) : RuntimeException(message)
class InternalServerErrorException(message: String) : RuntimeException(message)



/**
 * Trecho relacionado a [Application.module]
 * Referenced in application.conf
 */
@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    // Obtains the youkube config key from the application.conf file.
    // Inside that key, we then read several configuration properties
    // with the [session.cookie], the [key] or the [upload.dir]
    val registroDeComprasConfig = environment.config.config("registroDeCompras")
//    val sessionCookieConfig = youkubeConfig.config("session.cookie")
//    val key: String = sessionCookieConfig.property("key").getString()
//    val sessionkey = hex(key)

    // We create the folder and a [Database] in that folder for the configuration [upload.dir].
    val uploadDirPath: String = registroDeComprasConfig.property("upload.dir").getString()
    val uploadDir = File(uploadDirPath)
    if (!uploadDir.mkdirs() && !uploadDir.exists()) {
        throw IOException("Failed to create directory ${uploadDir.absolutePath}")
    }

    val cacheDirPath: String = registroDeComprasConfig.property("cache.dir").getString()
    val cacheDir = File(cacheDirPath)
    if (!cacheDir.mkdirs() && !cacheDir.exists()) {
        throw IOException("Failed to create directory ${cacheDir.absolutePath}")
    }

    /**
     * Pool of JDBC connections used.
     */
    val pool = ComboPooledDataSource().apply {
        driverClass = Driver::class.java.name
        jdbcUrl = "jdbc:h2:file:${uploadDir.canonicalFile.absolutePath}"
        user = ""
        password = ""
    }

    /**
     * Constructs a facade with the database, connected to the DataSource configured earlier with the [dir]
     * for storing the database.
     */
    val dao: DAOFacade = DAOFacadeCache(
            DAOFacadeDatabase(Database.connect(pool))
            , File(cacheDir.parentFile, "ehcache"))
    dao.init()

    // And we subscribe to the stop event of the application, so we can also close the [ComboPooledDataSource] [pool].
    environment.monitor.subscribe(ApplicationStopped) { pool.close() }

    // Now we call to a main with the dependencies as arguments.
    // Separating this function with its dependencies allows us to provide several modules with
    // the same code and different datasources living in the same application,
    // and to provide mocked instances for doing integration tests.
    mainWithDependencies(dao)
}


@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Application.mainWithDependencies(dao: DAOFacade) {
    install(DefaultHeaders)

    install(CallLogging)

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            dateFormat = DateFormat.getDateInstance()
        }
    }

    install(Locations)

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")
    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }
    // eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoibWF1cmljaW8ifQ.MHJseQHCQCTpWycVYb____vnKeWwPTXhOdhctEO7Jd8

    install(StatusPages) {
//        exception<BadRequestException> { exception ->
//            call.respond(HttpStatusCode.BadRequest, mapOf("OK" to false, "error" to (exception.message ?: "")))
//        }
        exception<InternalServerErrorException> { exception ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
    }

    routing {
        routeRegistrosDeCompra( dao, log )
    }
}