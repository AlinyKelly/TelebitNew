package utilitarios;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.CentralFaturamento;
import br.com.sankhya.modelcore.comercial.ConfirmacaoNotaHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused"})
public class JapeHelper {
    /**
     * Método para criar um novo registro na instância informada.
     *
     * @param values:   HashMap<String, Object>: Nomes e valores dos campos.
     * @param instance: String: instancia a ser criado o novo registro
     * @return String
     * @author Luis Ricardo Alves Santos
     */
    private static DynamicVO createNewLine(HashMap<String, Object> values, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        StringBuilder listValues = new StringBuilder();
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            FluidCreateVO fluidCreateVO = instanciaDAO.create();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidCreateVO.set(name, value);
                listValues.append(name).append("= ").append(value).append("\n");
            }
            return fluidCreateVO.save();
        } catch (Exception e) {
            throw new MGEModelException("createNewLine Error:" + e.getMessage() + "\n Values:\n" + listValues);
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Retorna o valor de um campo(PK)
     *
     * @param name      Nome da propriedade
     * @param it        DynamicVO do registro
     * @param instancia Instância - Default: Instância atual
     * @return [T]
     */
    @SuppressWarnings({"unchecked", "unused"})
    public static <T> T getCampo(String name, DynamicVO it, String instancia) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            DynamicVO fluidGetVO = instanciaDAO.findByPK(it.getPrimaryKey());
            return (T) fluidGetVO.getProperty(name);
        } catch (Exception e) {
            throw new MGEModelException("getCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Retorna o valor de um campo(Where)
     *
     * @param name      Nome do campo
     * @param where     Condição para retornar o registro
     * @param instancia Instância - Default: Instância atual
     * @return [T]
     */
    @SuppressWarnings({"unchecked", "unused"})
    public static <T> T getCampo(String name, String where, String instancia) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            return (T) fluidGetVO.getProperty(name);
        } catch (Exception e) {
            throw new MGEModelException("getCampo Error(Entity: " + instancia + "):" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }
    @SuppressWarnings({"unchecked", "unused"})
    public static <T> T getCampo(String name, String where, String instancia, boolean canBeNull) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            try {

                return (T) fluidGetVO.getProperty(name);
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            throw new MGEModelException("getCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Retorna o DynamicVO baseado em uma consulta
     *
     * @param instancia Nome da instancia
     * @param where     Condição para retornar o registro
     */
    @SuppressWarnings({"unused"})
    public static DynamicVO getVO(String instancia, String where) throws MGEModelException {
        DynamicVO dynamicVo;
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            dynamicVo = instanciaDAO.findOne(where);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            throw new MGEModelException("Erro getVO: " + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
        return dynamicVo;
    }

    public static boolean deleteVO(DynamicVO vo, String instance) throws MGEModelException {

        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper empresaDAO = JapeFactory.dao(instance);
            return empresaDAO.delete(vo.getPrimaryKey());
        } catch (Exception e) {
            throw new MGEModelException("deleteVO error(" + instance + "):" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Retorna os registros baseados em uma consulta
     *
     * @param instancia Nome da instancia
     * @param where     Condição para retornar o registro
     */
    public static Collection<DynamicVO> getVOs(String instancia, String where) throws MGEModelException {
        Collection<DynamicVO> dynamicVo;
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            dynamicVo = instanciaDAO.find(where);
        } catch (java.lang.Exception e) {
            throw new MGEModelException("Erro getVO: " + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
        return dynamicVo;
    }

    /**
     * Alterar o valor do campo informado
     *
     * @param name     Nome do campo
     * @param value    Valor
     * @param vo       DynamicVO do item a ser atualizado
     * @param instance Instância - Default: Instância atual
     */
    public static void setCampo(String name, Object value, DynamicVO vo, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdateByPK(vo.getPrimaryKey());
            fluidupdate.set(name, value);
            fluidupdate.update();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MGEModelException("setCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Alterar o valor do campo informado
     *
     * @param name     Nome do campo
     * @param value    Valor
     * @param where    condição a ser cumprida
     * @param instance Instância - Default: Instância atual
     */
    public static void setCampo(String name, Object value, String where, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdateByPK(fluidGetVO.getPrimaryKey());
            fluidupdate.set(name, value);
            fluidupdate.update();
        } catch (Exception e) {
            throw new MGEModelException("setCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Alterar o valor de mutiplos campos informados
     *
     * @param values   Hashmap com nome e valor do campo
     * @param vo       DynamicVO do item a ser atualizado
     * @param instance Instância - Default: Instância atual
     * @return DynamicVO
     */
    public static DynamicVO setCampos(HashMap<String, Object> values, DynamicVO vo, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdate(vo);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidupdate.set(name, value);
            }
            fluidupdate.update();
            return instanciaDAO.findByPK(vo.getPrimaryKey());
        } catch (Exception e) {
            throw new MGEModelException("setCampos Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Alterar o valor de mutiplos campos informados
     *
     * @param values   Hashmap com nome e valor do campo
     * @param where    condição do item a ser atualizado
     * @param instance Instância - Default: Instância atual
     * @return DynamicVO
     */
    public static DynamicVO setCampos(HashMap<String, Object> values, String where, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdate(fluidGetVO);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidupdate.set(name, value);
            }
            fluidupdate.update();
            return instanciaDAO.findOne(where);
        } catch (Exception e) {
            throw new MGEModelException("setCampos Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    public static class CreateNewLine {
        private String instance;
        private HashMap<String, Object> values = new HashMap<>();

        public CreateNewLine(String instance) {
            this.instance = instance;
        }

        public DynamicVO save() throws MGEModelException {
            return createNewLine(values, instance);
        }

        public void set(String label, Object value) {
            values.put(label, value);
        }

        public void flush() {
            values = new HashMap<>();
        }

        public void remove(String label) {
            values.remove(label);
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }
    }


    public static void confirmarNota(BigDecimal nuNota) throws Exception {
        BarramentoRegra barramentoConfirmacao = BarramentoRegra.build(CentralFaturamento.class,
                "regrasConfirmacaoSilenciosa.xml", AuthenticationInfo.getCurrent());
        barramentoConfirmacao.setValidarSilencioso(true);
        ConfirmacaoNotaHelper.confirmarNota(nuNota, barramentoConfirmacao);
    }

    public static void atualizarImpostos(BigDecimal nuNota) throws Exception {
        ImpostosHelpper impostohelp = new ImpostosHelpper();
        impostohelp.calcularImpostos(nuNota);// IPI, ICMS e etc..
    }

}