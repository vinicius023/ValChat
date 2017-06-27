import org.jgroups.Address;

import java.util.HashMap;
import java.util.Vector;

/**
 * Created by vinicius023 on 6/23/17.
 */
public class Grupo {

    String nome;
    String adm;
    HashMap<String,Address> membros;

    public Grupo() {
        this.nome = "";
        this.adm = "";
        this.membros = new HashMap<>();
    }
    public Grupo(String nome, String adm, HashMap<String,Address> membros) {
        this.nome = nome;
        this.adm = adm;
        this.membros = membros;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getAdm() {
        return adm;
    }

    public void setAdm(String adm) {
        this.adm = adm;
    }

    public HashMap<String, Address> getMembros() {
        return membros;
    }

    public void setMembros(HashMap<String, Address> membros) {
        this.membros = membros;
    }

    public Vector<Address> getEnderecos() {
        Vector<Address> vectorEnderecos = new Vector<>();
        for (String member : membros.keySet())
            vectorEnderecos.add(membros.get(member));
        return vectorEnderecos;
    }
}
