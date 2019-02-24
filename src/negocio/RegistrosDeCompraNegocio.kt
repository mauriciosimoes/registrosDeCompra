package com.example.negocio

import com.example.RouteRegistrosDeCompra
import com.example.dao.DAOFacade
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

            // insert conta resolverContaText( registroDeCompraLocal.contaText )
            val conta = "em dúvida..."

            val id: Int = dao.createRegistroDeConta(
                registroDeCompraLocal.oQueFoiComprado
            , registroDeCompraLocal.quantoFoi
            , registroDeCompraLocal.quantasVezes
            , registroDeCompraLocal.tag
            , registroDeCompraLocal.valorDaParcela
            , ""
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
                , ""
                , registroDeCompraLocal.dataDaCompra
                , registroDeCompraLocal.urlNfe
                , userIdPrincipal.name
            )

            call.respond(mapOf("OK" to true, "registroDeCompra" to registroDeCompraResponse))
        }
    }
}

