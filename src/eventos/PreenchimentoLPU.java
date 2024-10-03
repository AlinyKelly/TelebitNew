package eventos;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import utilitarios.JapeHelper;

import java.math.BigDecimal;

public class PreenchimentoLPU implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO projVO = (DynamicVO) persistenceEvent.getVo();

        BigDecimal numContrato = projVO.asBigDecimalOrZero("NUMCONTRATO");
        String chaveLPU = projVO.asString("AD_CHAVELPU");

        if(
                numContrato != null
                        &&
                        chaveLPU != null
                        && !chaveLPU.isEmpty()
        ){

            DynamicVO tcsPreVO = JapeHelper.getVO("PrecoContrato","AD_CHAVELPU LIKE '"+chaveLPU+"' AND NUMCONTRATO = " + numContrato );


            if (tcsPreVO != null){
                projVO.setProperty("VLRUNIT", tcsPreVO.asBigDecimalOrZero("VALOR"));
            }



        }

    }

    @Override
    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO projVO = (DynamicVO) persistenceEvent.getVo();

        BigDecimal numContrato = projVO.asBigDecimalOrZero("NUMCONTRATO");
        String chaveLPU = projVO.asString("AD_CHAVELPU");

        if(
                numContrato != null
                &&
                        chaveLPU != null
                && !chaveLPU.isEmpty()
        ){

            DynamicVO tcsPreVO = JapeHelper.getVO("PrecoContrato","NUMCONTRATO = " + numContrato + " AND CODPROd = (SELECT CODPROD FROM TCSPSC WHERE AD_CHAVELPU LIKE '"+chaveLPU+"' AND TCSPSC.NUMCONTRATO = TCSPRE.NUMCONTRATO) " );


            if (tcsPreVO != null){
                projVO.setProperty("VLRUNIT", tcsPreVO.asBigDecimalOrZero("VALOR"));
            }



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
