package com.example.negocio

import com.example.InternalServerErrorException
import com.example.RouteRegistrosDeCompra
import com.example.dao.DAOFacade
import com.example.model.Conta
import com.example.model.PostRegistrosDeCompra
import com.example.model.RegistroDeCompra
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.BadResponseStatusException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.defaultSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.response.readText
import io.ktor.features.BadRequestException
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.post
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.h2.util.JdbcUtils.serializer
import org.slf4j.Logger
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URL


@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.routeRegistrosDeCompra(dao: DAOFacade, log: Logger) {
    authenticate {
        post<RouteRegistrosDeCompra> {
            val token =
                call.request.header("Authorization") ?: error("Informação de autenticação não encontrada no POST.")
            val userIdPrincipal =
                call.principal<UserIdPrincipal>() ?: error("Informação de autenticação não encontrada no POST.")

            val post = try {
                call.receive<PostRegistrosDeCompra>()
            } catch (e: JsonParseException) {
                throw BadRequestException(e.toString())
            } catch (e: JsonMappingException) {
                throw BadRequestException(e.toString())
            } catch (e: MismatchedInputException) {
                throw BadRequestException(e.toString())
            } catch (e: MissingKotlinParameterException) {
                throw BadRequestException(e.toString())
            }

            val registroDeCompraLocal
                    = post.registroDeCompraDoPost.toRegistroDeCompra()

            val contaTextRecuperado: String
                    = recuperaContaText( registroDeCompraLocal.contaText, log, token )

            val id: Int = dao.createRegistroDeConta(
                registroDeCompraLocal.oQueFoiComprado
            , registroDeCompraLocal.quantoFoi
            , registroDeCompraLocal.quantasVezes
            , registroDeCompraLocal.tag
            , registroDeCompraLocal.valorDaParcela
            , contaTextRecuperado
            , registroDeCompraLocal.dataDaCompra
            , registroDeCompraLocal.urlNfe
            , userIdPrincipal.name
            )

            val registroDeCompraResponse = RegistroDeCompra( id
                , registroDeCompraLocal.oQueFoiComprado
                , registroDeCompraLocal.quantoFoi
                , registroDeCompraLocal.quantasVezes
                , registroDeCompraLocal.tag
                , registroDeCompraLocal.valorDaParcela
                , contaTextRecuperado
                , registroDeCompraLocal.dataDaCompra
                , registroDeCompraLocal.urlNfe
                , userIdPrincipal.name
            )

            call.respond(registroDeCompraResponse)
//            call.respond(mapOf("OK" to true, "registroDeCompra" to registroDeCompraResponse))
        }
    }
}


suspend fun recuperaContaText(
        contaTextLocal: String,
        log: Logger,
        token: String
        ): String {
    val contaText: String

    if (contaTextLocal.isEmpty()) {
        contaText = contaTextLocal
    } else {
        val client = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
                defaultSerializer()
            }
            engine {
                followRedirects =
                    true  // Follow HTTP Location redirects - default false. It uses the default number of redirects defined by Apache's HttpClient that is 50.

                // For timeouts: 0 means infinite, while negative value mean to use the system's default value
                socketTimeout = 10_000  // Max time between TCP packets - default 10 seconds
                connectTimeout = 10_000 // Max time to establish an HTTP connection - default 10 seconds
                connectionRequestTimeout =
                    20_000 // Max time for the connection manager to start a request - 20 seconds

                customizeClient {
                    // Apache's HttpAsyncClientBuilder
                    //setProxy(HttpHost("127.0.0.1", 8080))
                    setMaxConnTotal(1000) // Maximum number of socket connections.
                    setMaxConnPerRoute(100) // Maximum number of requests for a specific endpoint route.
                }
                customizeRequest {
                    // Apache's RequestConfig.Builder
//                    setAuthenticationEnabled(true)


                }

            }

//                            install(JsonFe) {
//                                serializer = GsonSerializer () //{
            //                        // .GsonBuilder
            //                        serializeNulls()
            //                        disableHtmlEscaping()
            //                    }
//                            }
        }



        val urlGetContas = "http://localhost:8081/contas/${contaTextLocal}"
        log.trace("passei por aqui: ${token}")
        val contaHttpResponse = GlobalScope.async {
            try {
                client.get<Conta>() {
                    url(URL(urlGetContas))
                    header("Authorization", token)
//                    header("password","123456")

                }
            } catch (e: BadResponseStatusException) {
                log.trace("passei por aqui: catch (e: BadResponseStatusException) ")
//                if ( e.statusCode != HttpStatusCode.InternalServerError ) {
                    throw InternalServerErrorException( urlGetContas + " >>> " + e.response.status +
                            " >>> " + e.response.readText())
//                }
            } catch (e: ConnectException) {
                throw InternalServerErrorException( urlGetContas + " >>> " + e.toString() )
            } catch (e: SocketTimeoutException) {
                throw InternalServerErrorException( urlGetContas + " >>> " + e.toString() )
            }
        }


        //            val message = client.post<HelloWorld> {
        //                url(URL("http://127.0.0.1:8080/"))
        //                contentType(ContentType.Application.Json)
        //                body = HelloWorld(hello = "world")
        //            }

        val contaHttpResponseDone = contaHttpResponse.await() // Suspension point.
        client.close()
        contaText = contaHttpResponseDone.text
    }
    return contaText
}

