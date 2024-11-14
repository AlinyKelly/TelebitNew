package eventos;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.ws.ServiceContext;
import com.google.gson.JsonObject;
import com.sankhya.util.TimeUtils;

public class AtualizaVencimento implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {

        DynamicVO finVo = (DynamicVO) persistenceEvent.getVo();
        ServiceContext ctx = ServiceContext.getCurrent();
        JsonObject request = ctx.getJsonRequestBody();

        if (request.has("prazo")) {

            String prazo = request.get("prazo").getAsString();
            finVo.setProperty("DTVENC", TimeUtils.toDateTimestamp(
                    prazo, "dd/MM/yyyy"
            ));


        }


    }

    @Override
    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {

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
