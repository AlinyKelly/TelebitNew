package eventos;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.ws.ServiceContext;
import com.google.gson.JsonObject;
import com.sankhya.util.TimeUtils;
import utilitarios.JapeHelper;

import java.math.BigDecimal;

import static org.hsqldb.HsqlDateTime.e;

public class PreencheNumcontrato implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        BigDecimal idAtividade = vo.asBigDecimalOrZero("AD_IDATIVIDADE");

        ServiceContext ctx = ServiceContext.getCurrent();
        JsonObject request = ctx.getJsonRequestBody();


        if (!idAtividade.equals(BigDecimal.ZERO)) {

            DynamicVO projVO = JapeHelper.getVO("AD_TGESPROJ", "IDATIVIDADE = " + idAtividade);

            BigDecimal numContrato = projVO.asBigDecimalOrZero("NUMCONTRATO");

            vo.setProperty("NUMCONTRATO", numContrato);

            if (request.has("emissao")) {
                String emissao = request.get("emissao").getAsString();
                vo.setProperty("DTNEG", TimeUtils.toDateTimestamp(emissao, "dd/MM/yyyy"));

            }

        }


    }

    @Override
    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        BigDecimal idAtividade = vo.asBigDecimalOrZero("AD_IDATIVIDADE");


        if (!idAtividade.equals(BigDecimal.ZERO)) {

            DynamicVO projVO = JapeHelper.getVO("AD_TGESPROJ", "IDATIVIDADE = " + idAtividade);

            BigDecimal numContrato = projVO.asBigDecimalOrZero("NUMCONTRATO");

            vo.setProperty("NUMCONTRATO", numContrato);


        }


    }

    @Override
    public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void afterUpdate(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void afterDelete(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext transactionContext) throws Exception {

    }
}
