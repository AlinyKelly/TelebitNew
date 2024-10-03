package botoes

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
import br.com.sankhya.extensions.actionbutton.ContextoAcao
import br.com.sankhya.jape.core.JapeSession
import br.com.sankhya.jape.core.JapeSession.SessionHandle
import br.com.sankhya.jape.vo.DynamicVO
import br.com.sankhya.jape.wrapper.JapeFactory
import br.com.sankhya.jape.wrapper.JapeWrapper
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO
import br.com.sankhya.modelcore.MGEModelException
import br.com.sankhya.ws.ServiceContext
import com.sankhya.util.TimeUtils
import org.apache.commons.io.FileUtils
import utilitarios.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/*
* Botão para importar as informações da BOQ
* */
class ImportarOS : AcaoRotinaJava {

    private var codImportador: BigDecimal? = null

    @Throws(MGEModelException::class, IOException::class)
    override fun doAction(contextoAcao: ContextoAcao) {
        println("Botao Importador BOQ")
        var hnd: JapeSession.SessionHandle? = null

        var ultimaLinhaJson: LinhaJson? = null

        val linhasSelecionadas = contextoAcao.linhas

        try {
            for (linha in linhasSelecionadas) {
                var count = 0

                codImportador = linha.getCampo("CODIMPORTACAO") as BigDecimal?

                val data = linha.getCampo("ARQUIVO") as ByteArray?
                val ctx = ServiceContext.getCurrent()
                val file = File(ctx.tempFolder, "IMPBOQ" + System.currentTimeMillis())
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
                            // The substituted value will be contained in the result variable
                            line = getReplaceFileInfo(line);
                        }

                        val json = trataLinha(line)
                        ultimaLinhaJson = json

                        val fornecedor = json.fornecedor.trim().toBigDecimal()
                        val cnpj = json.cnpj.trim()
                        val idAtividade = json.idAtividade.trim()
                        val solicitante = json.solicitante.trim()
                        val blm = json.blm.trim()
                        val previsaoFat = json.previsaoFat.trim()
                        val categoria = json.categoria.trim()
                        val projeto = json.projeto.trim()
                        val siteId = json.siteId.trim()
                        val endId = json.endId.trim()
                        val cidadePrest = json.cidadePrest.trim()
                        val poBoq = json.poBoq.trim()
                        val os = json.os.trim()
                        val quantidade = converterValorMonetario(json.quantidade.trim())
                        val vlrUnit = converterValorMonetario(json.vlrUnit.trim())
                        val servico = json.servico.trim().toBigDecimal()




//                        val statusFin: String
//
//                        when (statusBOQ) {
//                            "BOQ Solicitada" -> {
//                                statusFin = "1"
//                            }
//
//                            "Criado" -> {
//                                statusFin = "2"
//                            }
//
//                            "Pedido de PO" -> {
//                                statusFin = "3"
//                            }
//
//                            "PO Emitido" -> {
//                                statusFin = "4"
//                            }
//
//                            "Reprovado" -> {
//                                statusFin = "5"
//                            }
//
//                            "Revisado" -> {
//                                statusFin = "6"
//                            }
//
//                            "Revisão" -> {
//                                statusFin = "7"
//                            }
//
//                            "RP" -> {
//                                statusFin = "8"
//                            }
//
//                            "Ag. Faturamento" -> {
//                                statusFin = "9"
//                            }
//
//                            "Faturado" -> {
//                                statusFin = "10"
//                            }
//
//                            else -> {
//                                statusFin = "N"
//                            }
//                        }

                        val buscarInfos = retornaVO("AD_TGESPROJ", "IDATIVIDADE = ${idAtividade.toBigDecimal()}")
                        val nufap = buscarInfos?.asBigDecimal("NUFAP")
                        val etapa = buscarInfos?.asBigDecimal("NUMETAPA")
                        val codProj = buscarInfos?.asBigDecimal("CODPROJ")
                        val contrato = buscarInfos?.asBigDecimal("NUMCONTRATO")
                        val parceiro = buscarInfos?.asBigDecimal("CODPARC")
                        val empresa = buscarInfos?.asBigDecimal("CODEMP")
                        val codvol = buscarInfos?.asString("CODVOL")
                        val codProd = buscarInfos?.asBigDecimal("CODPROD")
                        val dataInclusao = buscarInfos?.asTimestamp("DATAINC")
                        val top = contextoAcao.getParametroSistema("TOPOS") as BigDecimal
                        val tipoVenda = contextoAcao.getParametroSistema("TIPNEGOS") as BigDecimal
                        var nuNotaOS = buscarInfos?.asBigDecimal("NUNOTAOS")
                        val oc = buscarInfos?.asString("OC")
                        val vlrUnitarioNet = buscarInfos?.asBigDecimalOrZero("VLRUNIT");


                        println("ID Atividade = $idAtividade")



                            println("Inserir a OS na tela de Gestão")

                            try {

                                JapeFactory.dao("AD_TGESPROJ").prepareToUpdateByPK(idAtividade.toBigDecimal())
                                    .set("OC", os)
                                    .set("CODPARCOS",fornecedor)
                                    .set("CNPJ",cnpj)
                                    .set("SOLICITANTE", solicitante)
                                    .set("BLM", blm)
                                    .set("PREVISAOFAT", stringToTimeStamp(previsaoFat))
                                    .set("CATEGORIA", categoria)
                                    .set("PROJETO", projeto)
                                    .set("QUANTIDADEOS", quantidade)
                                    .set("VLRUNITOS", vlrUnit)
                                    .set("CODPROD", servico)
                                    .update()

                            } catch (e: Exception) {
                                throw Exception("Não foi possível atualizar dados da OS na tela de Gestão: \n${e.localizedMessage}")
                            }
                        

                        //Criar o orçamento
                        if (nuNotaOS == null) {
                            println("GERAR O LANÇAMENTO DA OS AQUI")
                            // Criar o JSON com as informações necessárias para criar o lançamento
                            // Enviar o json e receber o NUNOTA enviado no retorno.
                            // Atualizar o campo NUNOTABOQ na tela de Gestão
                            // Se gerar algum erro no processo salvar o erro no campo de erro ou em  alguma tela de LOG



                            try {


                                val cabDAO: JapeWrapper = JapeFactory.dao("CabecalhoNota");
                                val novaCab = cabDAO.create()
                                novaCab.set("CODPARC", fornecedor)
                                novaCab.set("DTNEG", TimeUtils.getNow())
                                novaCab.set("CODTIPOPER", top)
                                novaCab.set("CODTIPVENDA", tipoVenda)
                                novaCab.set("CODVEND",BigDecimal.ZERO)
                                novaCab.set("CODEMP", empresa)
                                novaCab.set("TIPMOV", "O")
                                novaCab.set("NUMCONTRATO", contrato)
                                novaCab.set("CODPROJ", codProj)
                                novaCab.set("AD_NUFAP", nufap)
                                novaCab.set("AD_NUMETAPA", etapa)
                                novaCab.set("AD_IDATIVIDADE", BigDecimal(idAtividade))
                                novaCab.set("NUMNOTA",os.toBigDecimal())
                                val novaCabVO = novaCab.save();

                                nuNotaOS = novaCabVO.asBigDecimal("NUNOTA");

                                try {
                                    val iteDAO: JapeWrapper = JapeFactory.dao("ItemNota");
                                    val novoIte = iteDAO.create()
                                    novoIte.set("NUNOTA", nuNotaOS)
                                    novoIte.set("CODPROD", codProd)
                                    novoIte.set("QTDNEG", quantidade)
                                    novoIte.set("CODLOCALORIG", BigDecimal.ZERO)
                                    novoIte.set("CODVOL", codvol)
                                    novoIte.set("PERCDESC", BigDecimal.ZERO)
                                    novoIte.set("VLRUNIT", vlrUnit)
                                    novoIte.set("VLRTOT", vlrUnit.multiply(quantidade))
                                    novoIte.set("AD_NUMETAPA", etapa)
                                    novoIte.save()


                                } catch (e: Exception) {
                                    throw Exception("Não foi possível inserir item na nota da OS gerada: \n${e.localizedMessage}")
                                }


                                try{
                                    JapeHelper.atualizarImpostos(nuNotaOS);
                                } catch (e: Exception) {
                                    throw Exception("Não foi possível atualizar os impostos da nota da OS gerada: \n${e.localizedMessage}")
                                }


                                //confirmar nota
                                try {
                                    JapeHelper.confirmarNota(nuNotaOS)
                                } catch (e: Exception) {
                                    throw Exception("Não foi possível confirmar a nota da OS gerada: \n${e.localizedMessage}")
                                }



                                try {
                                    JapeFactory.dao("AD_TGESPROJ").prepareToUpdateByPK(BigDecimal(idAtividade))
                                        .set("NUNOTAOS", nuNotaOS)
                                        .update()
                                } catch (e: Exception) {
                                    throw Exception("Erro ao preencher o NUNOTA da OS: \n ${e.localizedMessage}")
                                }


                            } catch (e: Exception) {
                                throw Exception("Não foi possível criar a nota da OS: \n${e.localizedMessage} \n")
                            }


                        }

                        line = br.readLine()
                    }
                }
            }

        } catch (e: Exception) {
            //throw MGEModelException("$e $ultimaLinhaJson ")
            inserirErroLOG("${e.localizedMessage} - $ultimaLinhaJson ", "Importação - Erro na linha importada.")
        } finally {
            JapeSession.close(hnd)
        }

        //MENSAGEM DE RETORNO
        contextoAcao.setMensagemRetorno("Lançamento(s) processado(s)! Verifique a tela de Gestão de Projetos.")
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
            cells[1],
            cells[2],
            cells[3],
            cells[4],
            cells[5],
            cells[6],
            cells[7],
            cells[8],
            cells[9],
            cells[10],
            cells[11],
            cells[12],
            cells[13],
            cells[14],
            cells[15]
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

    /**
     * Converte um valor (String) com separador de milhares para [BigDecimal]
     * @author Aliny Sousa
     * @param str  Texto a ser convertido
     * @return [BigDecimal]
     */
    fun converterValorMonetario(valorMonetario: String): BigDecimal {
        val valorNumerico = valorMonetario.replace("\"", "").replace(".", "").replace(",", ".")
        return BigDecimal(valorNumerico)
    }

    /**
     * Converte uma data dd/mm/yyyy ou dd-mm-yyyy em timestamp
     * @author Luis Ricardo Alves Santos
     * @param strDate  Texto a ser convertido
     * @return [Timestamp]
     */
    fun stringToTimeStamp(strDate: String): Timestamp? {
        try {
            val formatter: DateFormat = SimpleDateFormat("dd/MM/yyyy")
            val date: Date = formatter.parse(strDate)
            return Timestamp(date.time)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Retorna a data atual
     */
    fun getDhAtual(): Timestamp {
        return Timestamp(System.currentTimeMillis())
    }

    fun formatarDataString(originalDateStr: String): Timestamp? {
        // Formato original da data
        val originalFormat = SimpleDateFormat("M/dd/yyyy", Locale.US)
        // Novo formato desejado
        val newFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        // Parse da data original
        val date: Date? = originalFormat.parse(originalDateStr)

        return if (date != null) {
            // Formatação da data para o novo formato (string)
            val formattedDateStr = newFormat.format(date)
            // Parse da string formatada para um Date
            val formattedDate: Date? = newFormat.parse(formattedDateStr)
            // Conversão do Date para Timestamp
            formattedDate?.let { Timestamp(it.time) }
        } else {
            null // Retorna null se a data original for inválida
        }

    }

    data class LinhaJson(
        val fornecedor: String,
        val cnpj: String,
        val idAtividade: String,
        val solicitante: String,
        val blm: String,
        val previsaoFat: String,
        val categoria: String,
        val projeto: String,
        val siteId: String,
        val endId: String,
        val cidadePrest: String,
        val poBoq: String,
        val os: String,
        val quantidade: String,
        val vlrUnit: String,
        val servico: String
    )

}