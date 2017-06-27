/**
 * Created by vinicius023 on 6/23/17.
 */

import org.jgroups.*;
import org.jgroups.util.*;
import java.util.*;
import org.jgroups.blocks.*;


public class ValChat extends ReceiverAdapter implements RequestHandler {

    JChannel canal;
    String nickname = "";
    MessageDispatcher  despachante;
    final String SAIR = "QUIT";
    final String MENU = "MENU";
    final String GRUPO = "GRUPO";

    private Vector<Address> cluster;

    private HashMap<String,Address> membros = new HashMap<String,Address>();
    private ArrayList<Grupo> grupos = new ArrayList<>();

    public void menuInicial() {

        Scanner entradaNick = new Scanner(System.in);
        System.out.println("Digite o nickname: ");
        nickname = entradaNick.nextLine();

    }

    public Integer menuTipoMensagem() {

        int opcao = -1;

        Scanner entradaMenu = new Scanner(System.in);
        System.out.println("1 - Enviar MULTICAST\n" +
                "2 - Enviar ANYCAST\n" +
                "3 - Enviar UNICAST\n" +
                "\n" +
                "0 - Sair");

        opcao = entradaMenu.nextInt();
        return opcao;
    }

    private void Start() throws Exception{

        menuInicial(); // Pede nickname do usuario

        canal = new JChannel("cast.xml");

        despachante = new MessageDispatcher(canal, null, null, this);

        despachante.setRequestHandler(this);
        despachante.setMessageListener(this);
        despachante.setMembershipListener(this);

        canal.setReceiver(this);
        canal.connect("ValChat");
        eventLoop();
        canal.close();
    }

    public Address membroChat() {

        Scanner teclado = new Scanner(System.in);

        System.out.println("Vizinhos:\n" +membros.keySet());

        System.out.println("Escolha um membro do CHAT: ");
        String nome = teclado.next();

        return membros.get(nome);
    }

    public  HashMap<String,Address> membrosGrupo() {

        HashMap<String,Address> membrosGrupo = new HashMap<>();

        Scanner teclado = new Scanner(System.in);

        System.out.println("Vizinhos:\n" +membros.keySet());

        System.out.println("Escolha os membros do grupo: (separando por ',' cada membro)");

        String elementos = teclado.nextLine();

        String[] members = elementos.split(",");

        for (String m : members) {
            membrosGrupo.put(m,membros.get(m));
        }

        return membrosGrupo;
    }

    private void eventLoop(){

        Scanner teclado = new Scanner(System.in);
        String msg = "";
        boolean continua=true;


        try{
            enviaNickname();

        }catch(Exception e) {
            System.err.println( "ERRO: " + e.toString() );
        }
        int op = -1;

        while(continua){

            System.out.println("MENU - troca o tipo de envio de mensagens / GRUPO - gerenciar grupos / QUIT - sair do chat\n");

            try {

                if(msg.contains(SAIR)){
                    continua = false;
                }else {
                    if (msg.contains(MENU)) {
                        op = menuTipoMensagem();
                        msg = "";
                    }else if (msg.contains(GRUPO)) {
                        menuGrupo();
                    }else {

                        try {

                            switch (op) {
                                case 1:
                                    System.out.print(">");
                                    System.out.flush();
                                    msg = teclado.nextLine();
                                    enviaMulticast(msg);
                                    break;
                                case 2:
                                    menuGrupo();
                                    break;
                                case 3:
                                    Address aux = membroChat();
                                    do {
                                        System.out.println("MENU - troca o tipo de envio de mensagens / GRUPO - gerenciar grupos / QUIT - sair do chat / NOVO - conversar com outro membro\n");
                                        System.out.print(">");
                                        System.out.flush();
                                        msg = teclado.nextLine();
                                        if (msg.contains("NOVO")) {
                                            aux = membroChat();
                                        }else if (!msg.contains(MENU) && !msg.contains(GRUPO) && !msg.contains(SAIR)) {
                                            enviaUnicast(aux, msg);
                                        }
                                    } while (!msg.contains(MENU) && !msg.contains(GRUPO) && !msg.contains(SAIR));

                                    break;
                                default:
                                    System.out.print(">");
                                    System.out.flush();
                                    msg = teclado.nextLine();
                                    enviaMulticast(msg);
                                    break;
                            }
                        } catch (Exception e) {
                            System.err.println("ERRO: " + e.toString());
                        }
                    }
                }

            }catch(Exception e) {
                System.err.println( "ERRO: " + e.toString() );
            }


        }
    }

    private Grupo getGrupo(String nome) {
        Grupo saida = new Grupo();

        for (Grupo g: this.grupos) {
            if (g.nome.equals(nome)) {
                saida = g;
            }
        }

        return saida;
    }

    private void menuGrupo() throws Exception {
        Scanner ler = new Scanner(System.in);
        Scanner texto = new Scanner(System.in);

        int escolha = -1;

        while (escolha!=0) {


            System.out.println( "1 - Criar um Grupo\t" +
                    "2 - Enviar Mensagem para um Grupo\t" +
                    "0 - Sair do gerenciador");

            escolha = ler.nextInt();

            switch (escolha) {
                case 1 :
                    System.out.println("Digite o nome do grupo: ");
                    String nomeGrupo = texto.nextLine();

                    Grupo group = new Grupo();
                    group.setNome(nomeGrupo);
                    group.setMembros(membrosGrupo());
                    this.grupos.add(group);
                    break;
                case 2 :
                    System.out.println("Digite o nome do grupo depois digite a mensagem");
                    String nome = texto.nextLine();
                    System.out.print(">");
                    String msg = texto.nextLine();
                    enviaAnycast(getGrupo(nome).getEnderecos(), msg);
                    break;
                case 0 :
                    break;
                default:
                    System.out.println("opcao invalida");
                    break;
            }
        }
    }

    // envia o proprio nick pra todos do cluster e passa a conhecer quem esta no cluster
    private void enviaNickname() throws Exception{

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, null, "novo_membro" + " === " + nickname);

        RequestOptions opcoes = new RequestOptions();
        opcoes.setFlags(Message.DONT_BUNDLE); // envia imediatamente, não agrupa várias mensagens numa só
        opcoes.setMode(ResponseMode.GET_ALL); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)

        opcoes.setAnycasting(false);

        RspList<String> respostasVizinhos = despachante.castMessage(null, mensagem, opcoes); //MULTICAST

        //conhece os vizinhos que estao no cluster
        String nicknameVizinho;
        for(Address vizinho : respostasVizinhos.keySet()){
            nicknameVizinho = (respostasVizinhos.get(vizinho)).getValue();
            if(!membros.containsKey(nicknameVizinho))
                membros.put( nicknameVizinho , vizinho );
        }

        System.out.println("Vizinhos:\n" +membros.keySet());
    }

    private void enviaMulticast(String conteudo) throws Exception{

        Address cluster = null; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, null, "{MULTICAST} " + nickname + " diz: " + conteudo);

        RequestOptions opcoes = new RequestOptions();
        opcoes.setFlags(Message.DONT_BUNDLE); // envia imediatamente, não agrupa várias mensagens numa só
        opcoes.setMode(ResponseMode.GET_ALL); // espera receber a resposta de TODOS membros (ALL, MAJORITY, FIRST, NONE)

        opcoes.setAnycasting(false);

        RspList respList = despachante.castMessage(null, mensagem, opcoes); //MULTICAST
        //System.out.println("==> Respostas do cluster ao MULTICAST:\n" +respList+"\n");
    }

    private void enviaAnycast(Collection<Address> grupo, String conteudo) throws Exception{

        Message mensagem=new Message(null, "{ ANYCAST } " + conteudo); //apesar do endereço ser null, se as opcoes contiverem anycasting==true enviará somente aos destinos listados

        RequestOptions opcoes = new RequestOptions();
        opcoes.setFlags(Message.DONT_BUNDLE); // envia imediatamente, não agrupa várias mensagens numa só
        opcoes.setMode(ResponseMode.GET_MAJORITY); // espera receber a resposta da maioria do grupo (ALL, MAJORITY, FIRST, NONE)

        opcoes.setAnycasting(true);

        RspList respList = despachante.castMessage(grupo, mensagem, opcoes); //ANYCAST
        //System.out.println("==> Respostas do grupo ao ANYCAST:\n" +respList+"\n");

    }

    private void enviaUnicast(Address destino, String conteudo) throws Exception{

        Address cluster = destino; //endereço null significa TODOS os membros do cluster
        Message mensagem=new Message(cluster, null, "{UNICAST} " + nickname + " diz: " + conteudo);

        RequestOptions opcoes = new RequestOptions();
        opcoes.setFlags(Message.DONT_BUNDLE); // envia imediatamente, não agrupa várias mensagens numa só
        opcoes.setMode(ResponseMode.GET_FIRST); // não espera receber a resposta do destino (ALL, MAJORITY, FIRST, NONE)

        String resp = despachante.sendMessage(mensagem, opcoes); //UNICAST
        //System.out.println("==> Respostas do membro ao UNICAST:\n" +resp+"\n");
    }

    public void receive(Message msg) { //exibe mensagens recebidas
        System.out.println(msg.getSrc() + " < " + msg.getObject());
    }

    public Object handle(Message msg) throws Exception{ // responde requisições recebidas
        String comando = (String) msg.getObject();

        if (comando.contains("novo_membro")) {

            System.out.println(comando+"\n");
            String args[] = comando.split(" === ");

            // trata comandos do chat usar / ou # ou algo pra identificar comandos
            if(args[0].equals("novo_membro") && msg.getSrc()!=canal.getAddress() ){
                membros.put(args[1],msg.getSrc());
                return nickname; //resposta à requisição contida na mensagem
            }
            else
                return " ? ";
        }else if (comando.equals(SAIR)) {
            System.exit(0);
            return "";
        }else if (comando.equals(MENU)) {
            // Perguntar depois
            return "";
        }else if (comando.equals(GRUPO)) {
            // Perguntar depois menuGrupo();
            return "";
        }else {
            System.out.println("<" + comando+"\n");
            return "";
        }
    }

    public void viewAccepted(View new_view) { //exibe alterações na composição do grupo
        cluster = new Vector<Address>(canal.getView().getMembers());
        System.out.print("Novo membro entrou no chat: " /* + new_view */);
        //System.out.println("Membros " + membros.keySet());

        //CUIDADO: se na nova visão um membro sair, deve-se remover o nickname dele do HashMap

    }

    public static void main(String[] args) throws Exception{

        ValChat chat = new ValChat();
        chat.Start();
    }

}
