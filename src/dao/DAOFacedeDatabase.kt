package com.example.dao

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import java.io.Closeable
import java.io.File
import org.joda.time.DateTime

/**
 * A DAO Facade interface for the Database. This allows to provide several implementations.
 *
 * In this case this is used to provide a Database-based implementation using Exposed,
 * and a cache implementation composing another another DAOFacade.
 */
interface DAOFacade : Closeable {
    /**
     * Initializes all the required data.
     * In this case this should initialize the Users and Kweets tables.
     */
    fun init()

    fun createRegistroDeConta(
        oQueFoiComprado: String,
        quantoFoi: Double,
        quantasVezes: Long,
        tag: String,
        valorDaParcela: Double,
        contaText: String,
        dataDaCompra: DateTime,
        urlNfe: String?,
        usuario: String
    ): Int

//    fun conta(contaId: Int): Conta?
//
//    fun conta(): ListIterator<Conta>

//    fun createConta(text: String, isDefaut: Boolean): Int

    // TODO retornar varios.Com o efeito de like
//    fun contaByText(text: String): Conta?

//

}

/**
 * Database implementation of the facade.
 * Uses Exposed, and either an in-memory H2 database or a file-based H2 database by default.
 * But can be configured.
 */
class DAOFacadeDatabase(val db: Database = Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")):
        DAOFacade {
    constructor(dir: File) : this(Database.connect("jdbc:h2:file:${dir.canonicalFile.absolutePath}", driver = "org.h2.Driver"))

    override fun init() {
        db.transaction {
            create(RegistrosDeCompra)
        }
    }

    override fun createRegistroDeConta(oQueFoiComprado: String
                                       , quantoFoi: Double
                                       , quantasVezes: Long
                                       , tag: String
                                       , valorDaParcela: Double
                                       , contaText: String
                                       , dataDaCompra: DateTime
                                       , urlNfe: String?
                                       , usuario: String): Int {
        return db.transaction {
            RegistrosDeCompra.insert {
                it[RegistrosDeCompra.oQueFoiComprado] = oQueFoiComprado
                it[RegistrosDeCompra.quantoFoi] = quantoFoi.toBigDecimal()
                it[RegistrosDeCompra.quantasVezes] = quantasVezes.toInt()
                it[RegistrosDeCompra.tag] = tag
                it[RegistrosDeCompra.valorDaParcela] = valorDaParcela.toBigDecimal()
                it[RegistrosDeCompra.contaText] = contaText
                it[RegistrosDeCompra.dataDaCompra] = dataDaCompra
                it[RegistrosDeCompra.urlNfe] = urlNfe
                it[RegistrosDeCompra.usuario] = usuario
            }.generatedKey ?: throw IllegalStateException("No generated key returned.Erro na geração da chave")
        }
    }

    override fun close() {
    }
}
