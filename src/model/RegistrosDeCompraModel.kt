package com.example.model

import io.ktor.features.BadRequestException
import io.ktor.util.KtorExperimentalAPI
import org.joda.time.DateTime
import java.io.Serializable


data class PostRegistrosDeCompra(val registroDeCompraDoPost: RegistroDeCompraDoPost) {
    data class RegistroDeCompraDoPost(
        val oQueFoiComprado: String
        , val quantoFoi: Double
        , val quantasVezes: Long?
        , val tag: String?
        //, val valorDaParcela: Double? // sera QuantoFoi / QuantasVezes
        , val contaText: String? // sera o ultimo utilizado ou marcada como padrao
        , val dataDaCompra: DateTime? // sera dia corrente
        , val urlNfe: String? // sera vazio mesmo
    ) {
        @KtorExperimentalAPI
        fun toRegistroDeCompra(): RegistroDeCompra {
            val oQueFoiCompradoLocal : String
            if ( oQueFoiComprado.isBlank() ) {
                throw BadRequestException("'oQueFoiComprado' está em branco.")
            } else {
                oQueFoiCompradoLocal = oQueFoiComprado
            }

            val quantoFoiLocal : Double = when {
                quantoFoi > 0 -> quantoFoi
                else -> throw BadRequestException("'quantoFoi' é menor ou igual a zero.")
            }

            val quantasVezesLocal: Long = quantasVezes ?: 1

            val tagLocal= tag ?: ""

            val valorDaParcelaLocal = quantoFoiLocal / quantasVezesLocal

            val contaTextLocal= contaText ?: ""

            val dataDaCompraLocal : DateTime = dataDaCompra ?: DateTime.now()
            // LocalDateTime.now()

            return RegistroDeCompra(null
                , oQueFoiCompradoLocal, quantoFoiLocal
                , quantasVezesLocal, tagLocal
                , valorDaParcelaLocal, contaTextLocal
                , dataDaCompraLocal, urlNfe
                , "")
        }

    }
}

data class RegistroDeCompra(
    val id: Int?
    , val oQueFoiComprado: String
    , val quantoFoi: Double
    , val quantasVezes: Long
    , val tag: String
    , val valorDaParcela: Double
    , val contaText: String
    , val dataDaCompra: DateTime
    , val urlNfe: String?
    , val usuario: String?
) : Serializable

data class Conta(val contaId: Int, val text: String, val isDefaut: Boolean) : Serializable



