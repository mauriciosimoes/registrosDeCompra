package com.example.negocio

import com.example.RouteRegistrosDeCompra
import com.example.dao.DAOFacade
import com.example.model.PostRegistrosDeCompra
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.features.BadRequestException
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI


@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.routeRegistrosDeCompra(dao: DAOFacade) {
    authenticate {
        post<RouteRegistrosDeCompra> {
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


            // resolverContaText( registroDeCompraLocal.contaText )

            // registroDeCompraLocal.usuario = userIdPrincipal.name

//            val id: Int = dao.createConta(it.text, it.isDefaut)
//            registrosDeCompra += RegistroDeCompra(post.registroDeCompraDoPost.oQueFoiComprado
//                , post.registroDeCompraDoPost.quantoFoi
//                , post.registroDeCompraDoPost.quantasVezes
//                , post.registroDeCompraDoPost.tag
//                , post.registroDeCompraDoPost.valorDaParcela
//                , post.registroDeCompraDoPost.contaText
//                , post.registroDeCompraDoPost.dataDaCompra
//                , post.registroDeCompraDoPost.urlNfe
//                , userIdPrincipal.name)
//            call.respond(mapOf("OK" to true))

            call.respond(mapOf("OK" to true, "registroDeCompraLocal" to registroDeCompraLocal))
        }
    }
}

