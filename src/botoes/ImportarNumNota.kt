package botoes

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
import br.com.sankhya.extensions.actionbutton.ContextoAcao
import br.com.sankhya.jape.core.JapeSession
import br.com.sankhya.jape.core.JapeSession.SessionHandle
import br.com.sankhya.jape.vo.DynamicVO
import br.com.sankhya.jape.wrapper.JapeFactory
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO
import br.com.sankhya.modelcore.MGEModelException
import br.com.sankhya.modelcore.util.DynamicEntityNames
import br.com.sankhya.ws.ServiceContext
import org.apache.commons.io.FileUtils
import utilitarios.JapeHelper
import utilitarios.getFluidCreateVO
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.regex.Matcher
import java.util.regex.Pattern

class ImportarNumNota : AcaoRotinaJava {

    private var codImportador: BigDecimal? = null

    override fun doAction(contextoAcao: ContextoAcao) {
        var hnd: JapeSession.SessionHandle? = null
        var ultimaLinhaJson: LinhaJson? = null
        val linhasSelecionadas = contextoAcao.linhas

        try {
            for (linha in linhasSelecionadas) {
                var count = 0

                codImportador = linha.getCampo("CODIMPORTACAO") as BigDecimal?

                val data = linha.getCampo("ARQUIVO") as ByteArray?
                val ctx = ServiceContext.getCurrent()
                val file = File(ctx.tempFolder, "IMPNUMNOTA" + System.currentTimeMillis())
                FileUtils.writeByteArrayToFile(file, data)

                hnd = JapeSession.open()

                BufferedReader(FileReader(file)).use { br ->
                    var line = br.readLine()

                    while (line != null) {
                        if (count == 0) {
                            count++
                            line = br.readLine()
                            continue
                        }
                        count++

                        if (line.contains("__end_fileinformation__")) {
                            line = getReplaceFileInfo(line)
                        }

                        val json = trataLinha(line)
                        ultimaLinhaJson = json

                        val nuNota = json.nunota.trim().toBigDecimal()
                        val numNota = json.numnota.trim().toBigDecimal()

                        val cabecalhoVO = retornaVO("CabecalhoNota", "NUNOTA = $nuNota")
                        val codtipoper = cabecalhoVO?.asBigDecimal("CODTIPOPER")
                        val statusnota = cabecalhoVO?.asString("STATUSNOTA")

                        //atualizacao aqui
                        if (codtipoper!!.intValueExact() == 1421 && statusnota != "L") {
                            atualizarNumnota(nuNota, numNota)

                            try {
                                JapeHelper.confirmarNota(nuNota)
                            } catch (e: Exception) {
                                throw  Exception("Não foi possível confirmar o Nro. Único $nuNota: \n${e.localizedMessage}")
                            }
                        }

                        line = br.readLine()
                    }

                }
            }

            contextoAcao.setMensagemRetorno("Atualização concluída!")

        } catch (e: Exception) {
            inserirErroLOG("${e.localizedMessage} - $ultimaLinhaJson ", "Importação - Erro na linha importada.")
        } finally {
            JapeSession.close(hnd)
        }


    }

    private fun atualizarNumnota(nunota: BigDecimal?, numnota: BigDecimal?) {
        var hnd: SessionHandle? = null
        try {
            hnd = JapeSession.open()

            JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA).prepareToUpdateByPK(nunota)
                .set("NUMNOTA", numnota)
                .update()
        } catch (e: Exception) {
            throw Exception(e)
        } finally {
            JapeSession.close(hnd)
        }
    }

    private fun getReplaceFileInfo(line: String): String {
        val regex = "__start_fileinformation__.*__end_fileinformation__"
        val subst = ""

        val pattern: Pattern = Pattern.compile(regex, Pattern.MULTILINE)
        val matcher: Matcher = pattern.matcher(line)

        return matcher.replaceAll(subst)
    }

    private fun trataLinha(linha: String): LinhaJson {
        var cells = if (linha.contains(";")) linha.split(";(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex()).toTypedArray()
        else linha.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex()).toTypedArray()

        cells = cells.filter { predicate ->
            if (predicate.isEmpty())
                return@filter false
            return@filter true
        }.toTypedArray() // Remove linhas vazias

        val ret = if (cells.isNotEmpty()) LinhaJson(
            cells[0],
            cells[1]
        ) else
            null

        if (ret == null) {
            //throw Exception("Erro ao processar a linha: $linha")
            // Salvar o erro de processamento da linha na tela detalhe de log.
            val erro = "Erro ao processar a linha: $linha"

            inserirErroLOG(erro, "Importação - Erro na linha importada.")
        }

        return ret!!
    }

    private fun inserirErroLOG(erro: String, origemErro: String) {
        val logErroLinha: FluidCreateVO = getFluidCreateVO("AD_LOGIMPGP")
        logErroLinha.set("CODIMPORTACAO", codImportador)
        logErroLinha.set("ERRO", erro)
        logErroLinha.set("DHERRO", getDhAtual())
        logErroLinha.set("ORIGEMERRO", origemErro)
        logErroLinha.save()
    }

    fun getDhAtual(): Timestamp {
        return Timestamp(System.currentTimeMillis())
    }

    @Throws(MGEModelException::class)
    fun retornaVO(instancia: String?, where: String?): DynamicVO? {
        var dynamicVo: DynamicVO? = null
        var hnd: JapeSession.SessionHandle? = null
        try {
            hnd = JapeSession.open()
            val instanciaDAO = JapeFactory.dao(instancia)
            dynamicVo = instanciaDAO.findOne(where)
        } catch (e: java.lang.Exception) {
            MGEModelException.throwMe(e)
        } finally {
            JapeSession.close(hnd)
        }
        return dynamicVo
    }

    data class LinhaJson(
        val nunota: String,
        val numnota: String
    )

}