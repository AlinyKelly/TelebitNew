package botoes

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
import br.com.sankhya.extensions.actionbutton.ContextoAcao
import br.com.sankhya.jape.core.JapeSession
import br.com.sankhya.jape.vo.DynamicVO
import br.com.sankhya.jape.wrapper.JapeFactory
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO
import br.com.sankhya.modelcore.MGEModelException
import br.com.sankhya.ws.ServiceContext
import com.sankhya.util.TimeUtils
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.io.FileUtils
import org.json.JSONObject
import utilitarios.JapeHelper
import utilitarios.JapeHelper.CreateNewLine
import utilitarios.getFluidCreateVO
import utilitarios.getPropFromJSON
import utilitarios.post
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

class ImportarFS : AcaoRotinaJava {

    private var codImportador: BigDecimal? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Timeout de conexão
        .writeTimeout(60, TimeUnit.SECONDS) // Timeout de escrita
        .readTimeout(60, TimeUnit.SECONDS) // Timeout de leitura
        .build()
    private val jSessionId: String = ServiceContext.getCurrent().httpSessionId
    private val url: String = ServiceContext.getCurrent().httpRequest.localAddr
    private val port = ServiceContext.getCurrent().httpRequest.localPort.toString()
    private val urlCompleta = "http://$url:$port"

    @Throws(MGEModelException::class, IOException::class)
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
                val file = File(ctx.tempFolder, "IMPFS" + System.currentTimeMillis())
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

                        val idAtividade = json.idatividade.trim()
                        val folhaSevico = json.fs.trim()
                        val qtdFS = converterValorMonetario(json.qtdFs.trim())
                        val emissaoFS = json.emissaoFS.trim()
                        val statusFS = json.statusFS.trim()
//                        val codigoServico = json.codigoservico.trim()
//                        val aliqISS = converterValorMonetario(json.aliqISS.trim())
//                        val aliqPIS = converterValorMonetario(json.aliqPIS.trim())
//                        val aliqCOFINS = converterValorMonetario(json.aliqCOFINS.trim())
//                        val aliqCSSL = converterValorMonetario(json.aliqCSSL.trim())
//                        val aliqIRRF = converterValorMonetario(json.aliqIRRF.trim())
//                        val aliqINSS = converterValorMonetario(json.aliqINSS.trim())

                        val buscarInfos = retornaVO("AD_TGESPROJ", "IDATIVIDADE = ${idAtividade.toBigDecimal()}")
                        val nunotaPO = buscarInfos?.asBigDecimal("NUNOTAPO")
                        val tipoOperacao = contextoAcao.getParametroSistema("TOPFS")
                        val serieTipoOperacao = contextoAcao.getParametroSistema("SERIETOPFS")
                        val nunotaFS = buscarInfos?.asBigDecimal("NUNOTAFS")
                        val nroFS = buscarInfos?.asString("NROFS")
                        val statusProd = buscarInfos?.asString("STATUSPROD")
                        val prazo = buscarInfos?.asTimestamp("DTPRAZO");

                        val listaFS = JapeHelper.getVO("AD_TGESPROJFS", "NUMFS = " + folhaSevico);

                        if (listaFS == null) {

                            var hndFS: JapeSession.SessionHandle? = null


                            try {


                                val jsonString = """{
                              "serviceName": "SelecaoDocumentoSP.faturar",
                              "requestBody": {
                              "emissao":"$emissaoFS",
                              "prazo":"${TimeUtils.formataDDMMYYYY(prazo)}",
                                "notas": {
                                  "codTipOper": $tipoOperacao,
                                  "dtFaturamento": "$emissaoFS",
                                  "serie": "$serieTipoOperacao",
                                  "dtSaida": "",
                                  "hrSaida": "",
                                  "tipoFaturamento": "FaturamentoNormal",
                                  "dataValidada": true,
                                  "notasComMoeda": {},
                                  "nota": [
                                    { "NUNOTA": "$nunotaPO", "itens": { "item": [{ "QTDFAT": $qtdFS, "$":1 }] } }
                                  ],
                                  "codLocalDestino": "",
                                  "conta2": 0,
                                  "faturarTodosItens": false,
                                  "umaNotaParaCada": "false",
                                  "ehWizardFaturamento": true,
                                  "dtFixaVenc": "",
                                  "ehPedidoWeb": false,
                                  "nfeDevolucaoViaRecusa": false,
                                  "isFaturamentoDanfeSeguranca": false
                                },
                                "clientEventList": {
                                  "clientEvent": [
                                    { "$": "br.com.sankhya.actionbutton.clientconfirm" },
                                    { "$": "br.com.sankhya.mgecom.enviar.recebimento.wms.sncm" },
                                    { "$": "comercial.status.nfe.situacao.diferente" },
                                    { "$": "br.com.sankhya.mgecom.compra.SolicitacaoComprador" },
                                    { "$": "br.com.sankhya.mgecom.expedicao.SolicitarUsuarioConferente" },
                                    { "$": "br.com.sankhya.mgecom.nota.adicional.SolicitarUsuarioGerente" },
                                    { "$": "br.com.sankhya.mgecom.cancelamento.nfeAcimaTolerancia" },
                                    { "$": "br.com.sankhya.mgecom.cancelamento.processo.wms.andamento" },
                                    { "$": "br.com.sankhya.mgecom.msg.nao.possui.itens.pendentes" },
                                    { "$": "br.com.sankhya.mgecomercial.event.baixaPortal" },
                                    { "$": "br.com.sankhya.mgecom.valida.ChaveNFeCompraTerceiros" },
                                    { "$": "br.com.sankhya.mgewms.expedicao.validarPedidos" },
                                    { "$": "br.com.sankhya.mgecom.gera.lote.xmlRejeitado" },
                                    { "$": "br.com.sankhya.comercial.solicitaContingencia" },
                                    { "$": "br.com.sankhya.mgecom.cancelamento.notas.remessa" },
                                    { "$": "br.com.sankhya.mgecomercial.event.compensacao.credito.debito" },
                                    {
                                      "$": "br.com.sankhya.modelcore.comercial.cancela.nota.devolucao.wms"
                                    },
                                    { "$": "br.com.sankhya.mgewms.expedicao.selecaoDocas" },
                                    { "$": "br.com.sankhya.mgewms.expedicao.cortePedidos" },
                                    {
                                      "$": "br.com.sankhya.modelcore.comercial.cancela.nfce.baixa.caixa.fechado"
                                    },
                                    { "$": "br.com.utiliza.dtneg.servidor" },
                                    {
                                      "$": "br.com.sankhya.mgecomercial.event.estoque.insuficiente.produto"
                                    }
                                  ]
                                }
                              }
                            }""".trimIndent()

                                val (postbody) = post(
                                    "mgecom/service.sbr?serviceName=SelecaoDocumentoSP.faturar&outputType=json",
                                    jsonString
                                )
                                val status = getPropFromJSON("status", postbody)

                                if (status == "1") {
                                    val nunotaRetorno = getPropFromJSON("responseBody.notas.nota.${'$'}", postbody)
                                    hndFS = JapeSession.open();

                                    val novaLinhaFS: CreateNewLine = JapeHelper.CreateNewLine("AD_TGESPROJFS");
                                    novaLinhaFS.set("NUMFS", folhaSevico.toBigDecimal())
                                    novaLinhaFS.set("IDATIVIDADE", idAtividade.toBigDecimal())
                                    novaLinhaFS.set("NUNOTAFS", nunotaRetorno.toBigDecimal())
                                    novaLinhaFS.set("QTDFS", qtdFS)
                                    novaLinhaFS.set("EMISSAOFS", stringToTimeStamp(emissaoFS))
                                    novaLinhaFS.set("STATUSFS", statusFS)
                                    novaLinhaFS.save()


                                    JapeHelper.setCampo("STATUSPROD", "10", buscarInfos, "AD_TGESPROJ");
                                    JapeHelper.setCampo(
                                        "AVANCO",
                                        getVlrAvanco(idAtividade),
                                        buscarInfos,
                                        "AD_TGESPROJ");


                                } else {
                                    val statusMessage = getPropFromJSON("statusMessage", postbody)
                                    inserirErroLOG(
                                        "ID Atividade nro $idAtividade - Erro: $statusMessage",
                                        "API - Erro ao criar Folha de Serviço."
                                    )
                                }


                            } catch (e: Exception) {
                                throw Exception("Erro ao criar FS" + e.message)
                            } finally {
                                JapeSession.close(hndFS)
                            }


                        }

                        line = br.readLine()
                    }

                }

            }
        } catch (e: Exception) {
            //throw MGEModelException("$e $ultimaLinhaJson")
            inserirErroLOG("$e $ultimaLinhaJson", "Importação - Erro na linha importada.")
        } finally {
            JapeSession.close(hnd)
        }

        contextoAcao.setMensagemRetorno("Lançamento(s) inserido(s) com sucesso! Verifique a tela de Gestão de Projetos.")
    }

    private fun atualizaEmissaoFS(nunotaRetorno: String, emissao: String) {

        val jsonString = """
                {
                  "serviceName": "ActionButtonsSP.executeJava",
                  "requestBody": {
                    "nunota":"$nunotaRetorno",
                    "prazo":"$emissao",
                    "javaCall": {
                      "actionID": "79"
                    }
                  }
                }
            """.trimIndent()

        // Monta o JSON usando JSONObject


        // Monta a requisição
        val mediaType = MediaType.parse("application/json; charset=utf-8")

        val body = RequestBody.create(mediaType, jsonString)
        val request: Request = Request.Builder()
            .url(urlCompleta + "/mge/service.sbr?serviceName=ActionButtonsSP.executeJava&outputType=json&mgeSession=" + jSessionId)
            .post(body)
            .addHeader("cookie", "JSESSIONID=$jSessionId.master")
            .addHeader("Content-Type", "application/json;charset=utf-8")
            .build()

        // Envia a requisição
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful()) {
                    val jsonResponse: JSONObject = JSONObject(response.body()?.string())

                    if (jsonResponse.has("responseBody")) {
                        val responseBody = jsonResponse.getJSONObject("responseBody")
                        val status = jsonResponse.getString("status")
                        if (status == "1") {

                        } else {
                            throw java.lang.Exception("Erro ao atualizar data de negociação da FS:$jsonResponse")
                        }
                    } else {
                        throw java.lang.Exception("Erro ao atualizar data de negociação da FS II:$jsonResponse")
                    }
                } else {
                    throw java.lang.Exception(
                        """
                        Erro ao iniciar processo: 
                        response:${response.body()?.string()}
                        """.trimIndent()
                    )
                }
            }
        } catch (e: java.lang.Exception) {
            throw java.lang.Exception( "Erro ao atualizar vencimento da PO: ${e.localizedMessage}" )
        }



    }

    private fun getVlrAvanco(idAtividade: String): Any? {

        val tgesProj = JapeHelper.getVO("AD_TGESPROJ", "IDATIVIDADE = " + idAtividade.toBigDecimal());
        val qtdNeg = tgesProj?.asBigDecimal("QTDNEG");
        val tgesProjFS = JapeHelper.getVOs("AD_TGESPROJFS", "IDATIVIDADE = " + idAtividade.toBigDecimal());
        var qtdFS = BigDecimal.ZERO;

        for (fs in tgesProjFS) {
            qtdFS = qtdFS.add(fs.asBigDecimal("QTDFS"));
        }


        return qtdFS.divide(qtdNeg).multiply(BigDecimal(100));
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
            cells[11]
        ) else
            null

        if (ret == null) {
//            throw Exception("Erro ao processar a linha: $linha")
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
     * Converte uma data dd/mm/yyyy ou dd-mm-yyyy em timestampb
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
        val idatividade: String,
        val fs: String,
        val qtdFs: String,
        val emissaoFS: String,
        val statusFS: String,
        val codigoservico: String,
        val aliqISS: String,
        val aliqPIS: String,
        val aliqCOFINS: String,
        val aliqCSSL: String,
        val aliqIRRF: String,
        val aliqINSS: String
    )
}